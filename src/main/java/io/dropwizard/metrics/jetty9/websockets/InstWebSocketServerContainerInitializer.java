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
package io.dropwizard.metrics.jetty9.websockets;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.metrics.jetty9.websockets.annotated.InstJsrServerEndpointImpl;
import io.dropwizard.metrics.jetty9.websockets.endpoint.InstJsrServerExtendsEndpointImpl;
import javax.servlet.ServletException;
import org.eclipse.jetty.websocket.common.events.EventDriverFactory;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.server.NativeWebSocketConfiguration;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;

public class InstWebSocketServerContainerInitializer {
    public static ServerContainer configureContext(final MutableServletContextHandler context, final MetricRegistry metrics) throws ServletException {
        WebSocketUpgradeFilter filter = WebSocketUpgradeFilter.configureContext(context);
        NativeWebSocketConfiguration wsConfig = filter.getConfiguration();
        
        ServerContainer wsContainer = new ServerContainer(wsConfig, context.getServer().getThreadPool());
        EventDriverFactory edf = wsConfig.getFactory().getEventDriverFactory();
        edf.clearImplementations();

        edf.addImplementation(new InstJsrServerEndpointImpl(metrics));
        edf.addImplementation(new InstJsrServerExtendsEndpointImpl(metrics));
        context.addBean(wsContainer);
        context.setAttribute(javax.websocket.server.ServerContainer.class.getName(), wsContainer);
        context.setAttribute(WebSocketUpgradeFilter.class.getName(), filter);
        return wsContainer;
    }
}
