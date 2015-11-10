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

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.eclipse.jetty.websocket.jsr356.server.BasicServerEndpointConfig;

public class MyApp extends Application<Configuration> {

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new WebsocketBundle(
                Arrays.asList(AnnotatedEchoServer.class),
                Arrays.asList(new BasicServerEndpointConfig(EchoServer.class, "/extends-ws"))));
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws InvalidKeySpecException, NoSuchAlgorithmException, ServletException, DeploymentException {
        environment.jersey().register(new MyResource());
        environment.healthChecks().register("alive", new HealthCheck() {
            @Override
            protected HealthCheck.Result check() throws Exception {
                return HealthCheck.Result.healthy();
            }
        });
    }

    @Metered
    @Timed
    @ServerEndpoint("/annotated-ws")
    public static class AnnotatedEchoServer {
        @OnOpen
        public void myOnOpen(final Session session) throws IOException {
            session.getAsyncRemote().sendText("welcome");
        }

        @OnMessage
        public void myOnMsg(final Session session, String message) {
            session.getAsyncRemote().sendText(message.toUpperCase());
        }

        @OnClose
        public void myOnClose(final Session session, CloseReason cr) {
        }
    }

    @Metered
    @Timed
    public static class EchoServer extends Endpoint implements MessageHandler.Whole<String> {
        private Session session;

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            session.getAsyncRemote().sendText("welcome");
            this.session = session;
        }

        @Override
        public void onMessage(String message) {
            session.getAsyncRemote().sendText(message.toUpperCase());
        }
    }

    @Path("/api")
    @Produces(value = MediaType.APPLICATION_JSON)
    public static class MyResource {

        @Metered
        @GET
        public String get(@QueryParam(value = "name") String name) throws Exception {
            return "hello " + name;
        }
    }
}
