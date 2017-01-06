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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.dropwizard.metrics.jetty9.websockets.EventDriverMetrics;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.websocket.CloseReason;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.jsr356.endpoints.EndpointInstance;
import org.eclipse.jetty.websocket.jsr356.endpoints.JsrEndpointEventDriver;

public class InstJsrEndpointEventDriver extends JsrEndpointEventDriver {
    private final EventDriverMetrics edm;

    public InstJsrEndpointEventDriver(WebSocketPolicy policy, EndpointInstance ei, MetricRegistry metrics) {
        super(policy, ei);
        this.edm = new EventDriverMetrics(metadata.getEndpointClass(), metrics);
    }

    @Override
    public void onTextFrame(ByteBuffer buffer, boolean fin) throws IOException {
        super.onTextFrame(buffer, fin); 
        if (activeMessage==null) // finished message
            edm.onTextMeter.ifPresent(Meter::mark);
    }

    @Override
    public void onError(Throwable cause) {
        edm.exceptionMetered.ifPresent(Meter::mark);
        super.onError(cause); 
    }

    @Override
    public void onConnect() {
        edm.countOpened.ifPresent(Counter::inc);
        edm.timer.ifPresent(e -> getJsrSession().getUserProperties().put(this.getClass().getName(), e.time()));
        super.onConnect();
    }

    @Override
    protected void onClose(CloseReason closereason) {
        edm.countOpened.ifPresent(Counter::dec);
        Timer.Context ctx = (Timer.Context) getJsrSession().getUserProperties().get(this.getClass().getName());
        if (ctx != null)
            ctx.close();
        super.onClose(closereason);
    }
}
