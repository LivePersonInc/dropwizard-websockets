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
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static io.dropwizard.websockets.GeneralUtils.rethrow;

public class WebsocketBundle implements Bundle {

    private final Collection<ServerEndpointConfig> endpointConfigs = new ArrayList<>();
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketBundle.class);
    volatile boolean starting = false;
    private ServerEndpointConfig.Configurator defaultConfigurator;


    public WebsocketBundle(ServerEndpointConfig.Configurator defaultConfigurator, Class<?>... endpoints) {
        this(defaultConfigurator, Arrays.asList(endpoints), new ArrayList<>());
    }

    public WebsocketBundle(Class<?>... endpoints) {
        this(null, Arrays.asList(endpoints), new ArrayList<>());
    }

    public WebsocketBundle(ServerEndpointConfig... configs) {
        this(null, new ArrayList<>(), Arrays.asList(configs));
    }

    public WebsocketBundle(ServerEndpointConfig.Configurator defaultConfigurator, Collection<Class<?>> endpointClasses, Collection<ServerEndpointConfig> serverEndpointConfigs) {
        this.defaultConfigurator = defaultConfigurator;
        endpointClasses.forEach((clazz)-> addEndpoint(clazz));
        this.endpointConfigs.addAll(serverEndpointConfigs);
    }

    public void addEndpoint(ServerEndpointConfig epC) {
        endpointConfigs.add(epC);
        if (starting)
            throw new RuntimeException("can't add endpoint after starting lifecycle");
    }

    public void addEndpoint(Class<?> clazz) {
        ServerEndpoint anno = clazz.getAnnotation(ServerEndpoint.class);
        if(anno == null){
            throw new RuntimeException(clazz.getCanonicalName()+" does not have a "+ServerEndpoint.class.getCanonicalName()+" annotation");
        }
        ServerEndpointConfig.Builder bldr =  ServerEndpointConfig.Builder.create(clazz, anno.value());
        if(defaultConfigurator != null){
            bldr = bldr.configurator(defaultConfigurator);
        }
        endpointConfigs.add(bldr.build());
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
                    endpointConfigs.forEach(rethrow(conf -> addEndpoint(wsContainer, conf, sb)));
                    LOG.info(sb.toString());
                } catch (ServletException ex) {
                    throw new RuntimeException(ex);
                }
            }

            private void addEndpoint(ServerContainer wsContainer, ServerEndpointConfig conf, StringBuilder sb) throws DeploymentException {
                wsContainer.addEndpoint(conf);
                sb.append(String.format("    WS      %s (%s)", conf.getPath(), conf.getEndpointClass().getName())).append(System.lineSeparator());
            }
        });
    }

}
