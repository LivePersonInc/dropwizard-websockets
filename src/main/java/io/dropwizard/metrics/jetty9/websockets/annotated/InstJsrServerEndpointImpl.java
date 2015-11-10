/**
 * The MIT License
 * Copyright (c) 2015 LivePerson, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.dropwizard.metrics.jetty9.websockets.annotated;

import com.codahale.metrics.MetricRegistry;
import static io.dropwizard.websockets.GeneralUtils.rethrow;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.common.events.EventDriver;
import org.eclipse.jetty.websocket.common.events.EventDriverImpl;
import org.eclipse.jetty.websocket.jsr356.annotations.JsrEvents;
import org.eclipse.jetty.websocket.jsr356.annotations.OnMessageCallable;
import org.eclipse.jetty.websocket.jsr356.endpoints.EndpointInstance;
import org.eclipse.jetty.websocket.jsr356.endpoints.JsrAnnotatedEventDriver;
import org.eclipse.jetty.websocket.jsr356.server.AnnotatedServerEndpointMetadata;
import org.eclipse.jetty.websocket.jsr356.server.JsrServerEndpointImpl;
import org.eclipse.jetty.websocket.jsr356.server.PathParamServerEndpointConfig;

public class InstJsrServerEndpointImpl implements EventDriverImpl {
    private final MetricRegistry metrics;
    private final JsrServerEndpointImpl origImpl;
    private final Method getMaxMessageSizeMethod;

    public InstJsrServerEndpointImpl(MetricRegistry metrics) {
        super();
        this.metrics = metrics;
        this.origImpl = new JsrServerEndpointImpl();
        this.getMaxMessageSizeMethod = rethrow(() -> this.origImpl.getClass().getDeclaredMethod("getMaxMessageSize",int.class,OnMessageCallable[].class)).get();
        getMaxMessageSizeMethod.setAccessible(true);
    }

    @Override
    public EventDriver create(Object websocket, WebSocketPolicy policy) throws Throwable {
        if (!(websocket instanceof EndpointInstance)) {
            throw new IllegalStateException(String.format("Websocket %s must be an %s", websocket.getClass().getName(), EndpointInstance.class.getName()));
        }

        EndpointInstance ei = (EndpointInstance) websocket;
        AnnotatedServerEndpointMetadata metadata = (AnnotatedServerEndpointMetadata) ei.getMetadata();
        JsrEvents<ServerEndpoint, ServerEndpointConfig> events = new JsrEvents<>(metadata);

        // Handle @OnMessage maxMessageSizes
        int maxBinaryMessage = getMaxMessageSize(policy.getMaxBinaryMessageSize(), metadata.onBinary, metadata.onBinaryStream);
        int maxTextMessage = getMaxMessageSize(policy.getMaxTextMessageSize(), metadata.onText, metadata.onTextStream);

        policy.setMaxBinaryMessageSize(maxBinaryMessage);
        policy.setMaxTextMessageSize(maxTextMessage);

        //////// instrumentation is here
        JsrAnnotatedEventDriver driver = new InstJsrAnnotatedEventDriver(policy, ei, events, metrics);
        ////////
        
        // Handle @PathParam values
        ServerEndpointConfig config = (ServerEndpointConfig) ei.getConfig();
        if (config instanceof PathParamServerEndpointConfig) {
            PathParamServerEndpointConfig ppconfig = (PathParamServerEndpointConfig) config;
            driver.setPathParameters(ppconfig.getPathParamMap());
        }

        return driver;
    }

    @Override
    public String describeRule() {
        return origImpl.describeRule();
    }

    private int getMaxMessageSize(int defaultMaxMessageSize, OnMessageCallable... onMessages) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return (int) getMaxMessageSizeMethod.invoke(origImpl, defaultMaxMessageSize, onMessages);        
    }

    @Override
    public boolean supports(Object websocket) {
        return origImpl.supports(websocket);
    }
}
