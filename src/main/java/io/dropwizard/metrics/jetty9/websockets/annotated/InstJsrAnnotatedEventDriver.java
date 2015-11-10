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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;
import io.dropwizard.metrics.jetty9.websockets.EventDriverMetrics;
import javax.websocket.CloseReason;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.jsr356.annotations.JsrEvents;
import org.eclipse.jetty.websocket.jsr356.endpoints.EndpointInstance;
import org.eclipse.jetty.websocket.jsr356.endpoints.JsrAnnotatedEventDriver;

public class InstJsrAnnotatedEventDriver extends JsrAnnotatedEventDriver {
    private final EventDriverMetrics edm;

    public InstJsrAnnotatedEventDriver(WebSocketPolicy policy, EndpointInstance ei, JsrEvents<ServerEndpoint, ServerEndpointConfig> events, MetricRegistry metrics) {
        super(policy, ei, events);
        this.edm = new EventDriverMetrics(metadata.getEndpointClass(), metrics);
    }

    @Override
    public void onTextMessage(String message) {
        edm.onTextMeter.ifPresent(Meter::mark);
        super.onTextMessage(message);
    }

    @Override
    public void onConnect() {
        edm.countOpened.ifPresent(Counter::inc);
        edm.timer.ifPresent(e -> getJsrSession().getUserProperties().put(this.getClass().getName(), e.time()));
        super.onConnect();
    }

    @Override
    public void onError(Throwable cause) {
        edm.exceptionMetered.ifPresent(Meter::mark);
        super.onError(cause);
    }

    @Override
    protected void onClose(CloseReason closereason) {
        edm.countOpened.ifPresent(Counter::dec);
        Context ctx = (Context) getJsrSession().getUserProperties().get(this.getClass().getName());
        if (ctx != null)
            ctx.close();
        super.onClose(closereason);
    }

}
