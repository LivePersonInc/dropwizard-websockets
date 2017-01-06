/**
 * The MIT License
 * Copyright (c) 2017 LivePerson, Inc.
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
package io.dropwizard.metrics.jetty9.websockets.endpoint;

import com.codahale.metrics.MetricRegistry;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.common.events.EventDriver;
import org.eclipse.jetty.websocket.common.events.EventDriverImpl;
import org.eclipse.jetty.websocket.jsr356.endpoints.EndpointInstance;
import org.eclipse.jetty.websocket.jsr356.endpoints.JsrEndpointEventDriver;
import org.eclipse.jetty.websocket.jsr356.server.JsrServerExtendsEndpointImpl;
import org.eclipse.jetty.websocket.jsr356.server.PathParamServerEndpointConfig;

public class InstJsrServerExtendsEndpointImpl implements EventDriverImpl {
    private final MetricRegistry metrics;
    private final JsrServerExtendsEndpointImpl origImpl;

    public InstJsrServerExtendsEndpointImpl(MetricRegistry metrics) {
        this.metrics = metrics;
        this.origImpl = new JsrServerExtendsEndpointImpl();
    }

    @Override
    public EventDriver create(Object websocket, WebSocketPolicy policy)
    {
        if (!(websocket instanceof EndpointInstance))
        {
            throw new IllegalStateException(String.format("Websocket %s must be an %s",websocket.getClass().getName(),EndpointInstance.class.getName()));
        }
        
        EndpointInstance ei = (EndpointInstance)websocket;
        JsrEndpointEventDriver driver = new InstJsrEndpointEventDriver(policy, ei, metrics);
        
        ServerEndpointConfig config = (ServerEndpointConfig)ei.getConfig();
        if (config instanceof PathParamServerEndpointConfig)
        {
            PathParamServerEndpointConfig ppconfig = (PathParamServerEndpointConfig)config;
            driver.setPathParameters(ppconfig.getPathParamMap());
        }

        return driver;
    }

    @Override
    public String describeRule()
    {
        return origImpl.describeRule();
    }

    @Override
    public boolean supports(Object websocket)
    {
        return origImpl.supports(websocket);
    }
}
