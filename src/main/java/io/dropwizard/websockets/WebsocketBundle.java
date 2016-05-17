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
package io.dropwizard.websockets;

import io.dropwizard.Bundle;
import io.dropwizard.metrics.jetty9.websockets.InstWebSocketServerContainerInitializer;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import static io.dropwizard.websockets.GeneralUtils.rethrow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.ServerEndpointMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.dropwizard.websockets.GeneralUtils.rethrow;

public class WebsocketBundle implements Bundle {

    private final Collection<Class<?>> annotatedEndpoints = new ArrayList<>();
    private final Collection<ServerEndpointConfig> extendsEndpoints = new ArrayList<>();
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketBundle.class);
    volatile boolean starting = false;

    public WebsocketBundle(Class<?>... endpoints) {
        this(Arrays.asList(endpoints), new ArrayList<>());
    }

    public WebsocketBundle(ServerEndpointConfig... configs) {
        this(new ArrayList<>(), Arrays.asList(configs));
    }

    public WebsocketBundle(Collection<Class<?>> endClassCls, Collection<ServerEndpointConfig> epC) {
        this.annotatedEndpoints.addAll(endClassCls);
        this.extendsEndpoints.addAll(epC);
    }

    public void addEndpoint(ServerEndpointConfig epC) {
        extendsEndpoints.add(epC);
        if (starting)
            throw new RuntimeException("can't add endpoint after starting lifecycle");
    }

    public void addEndpoint(Class<?> endClassCls) {
        annotatedEndpoints.add(endClassCls);
        if (starting)
            throw new RuntimeException("can't add endpoint after starting lifecycle");
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(Environment environment) {
        environment.lifecycle().addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {

            @Override
            public void lifeCycleStarting(LifeCycle event) {
                starting = true;
                try {
                    ServerContainer wsContainer = InstWebSocketServerContainerInitializer.
                            configureContext(environment.getApplicationContext(), environment.metrics());

                    StringBuilder sb = new StringBuilder("Registering websocket endpoints: ")
                            .append(System.lineSeparator())
                            .append(System.lineSeparator());

                    annotatedEndpoints.forEach(rethrow(ep -> addEndpoint(wsContainer, ep, null, sb)));
                    extendsEndpoints.forEach(rethrow(conf -> addEndpoint(wsContainer, conf.getEndpointClass(), conf, sb)));
                    LOG.info(sb.toString());
                } catch (ServletException ex) {
                    throw new RuntimeException(ex);
                }
            }

            private void addEndpoint(ServerContainer wsContainer, final Class<?> endpointClass, ServerEndpointConfig conf, StringBuilder sb) throws DeploymentException {
                ServerEndpointMetadata md = wsContainer.getServerEndpointMetadata(endpointClass, conf);
                wsContainer.addEndpoint(md);
                sb.append(String.format("    WS      %s (%s)", md.getPath(), endpointClass.getName())).append(System.lineSeparator());
            }
        });
    }

}
