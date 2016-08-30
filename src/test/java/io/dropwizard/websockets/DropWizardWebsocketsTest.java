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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import static java.util.concurrent.TimeUnit.SECONDS;
import javax.websocket.Session;
import junit.framework.Assert;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.ext.client.java8.SessionBuilder;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DropWizardWebsocketsTest {
    @BeforeClass
    public static void setUpClass() throws InterruptedException, IOException {
        CountDownLatch serverStarted = new CountDownLatch(1);
        Thread serverThread = new Thread(GeneralUtils.rethrow(() -> new MyApp(serverStarted).run(new String[]{"server", Resources.getResource("server.yml").getPath()})));
        serverThread.setDaemon(true);
        serverThread.start();
        serverStarted.await(10, SECONDS);
    }
    private static final String TRAVIS_ENV = "TRAVIS_ENV";
    private CloseableHttpClient client;
    private ObjectMapper om;
    private ClientManager wsClient;

    @Before
    public void setUp() throws Exception {

        this.client = HttpClients.custom()
                .setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy(3, 2000))
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setSocketTimeout(10000)
                        .setConnectTimeout(10000)
                        .setConnectionRequestTimeout(10000)
                        .build()).build();
        this.om = new ObjectMapper();
        this.wsClient = ClientManager.createClient();
        wsClient.getProperties().put(ClientProperties.HANDSHAKE_TIMEOUT, 10000);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testGet() throws IOException, InterruptedException, Exception {
        final int NUM = 2;
        for (int i = 0; i < NUM; i++) {
            if (System.getProperty(TRAVIS_ENV) != null) {
                System.out.println("waiting for Travis machine");
                Thread.sleep(1); // Ugly sleep to debug travis            
            }
            assertTrue(client.execute(new HttpGet(String.format("http://%s:%d/api?name=foo", LOCALHOST, PORT)), BASIC_RESPONSE_HANDLER).contains("foo"));
        }
        ObjectNode json = om.readValue(client.execute(new HttpGet(METRICS_URL), BASIC_RESPONSE_HANDLER), ObjectNode.class);
        Assert.assertEquals(NUM,
                json.path("meters").path(MyApp.MyResource.class.getName() + ".get").path("count").asInt());

    }

    @Test
    public void testAnnotatedWebsocket() throws Exception {
        testWsMetrics(MyApp.AnnotatedEchoServer.class, "annotated-ws");
    }

    @Test
    public void testExtendsWebsocket() throws Exception {
        testWsMetrics(MyApp.EchoServer.class, "extends-ws");
    }

    private void testWsMetrics(final Class<?> klass, final String path) throws Exception {
        try (Session ws = new SessionBuilder(wsClient)
                .uri(new URI(String.format("ws://%s:%d/%s", LOCALHOST, PORT, path)))
                .connect()) {
            for (int i = 0; i < 3; i++) {
                ws.getAsyncRemote().sendText("hello");
            }
            ObjectNode json = om.readValue(client.execute(new HttpGet(METRICS_URL), BASIC_RESPONSE_HANDLER), ObjectNode.class);
            // One open connection
            Assert.assertEquals(1,
                    json.path("counters").path(klass.getName() + ".openConnections").path("count").asInt());
        }
        ObjectNode json = om.readValue(client.execute(new HttpGet(METRICS_URL), BASIC_RESPONSE_HANDLER), ObjectNode.class);

        // Number of sessions that were opened
        Assert.assertEquals(1,
                json.path("timers").path(klass.getName()).path("count").asInt());

        // Length of session should be 5ms
        Assert.assertEquals(0.05, json.path("timers").path(klass.getName()).path("max").asDouble(), 1);

        // No Open connections
        Assert.assertEquals(0,
                json.path("counters").path(klass.getName() + ".openConnections").path("count").asInt());

        // Three text messages
        Assert.assertEquals(3,
                json.path("meters").path(klass.getName() + ".OnMessage").path("count").asInt());
    }

    public static void waitUrlAvailable(final String url) throws InterruptedException, IOException {
        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);
            try {
                if (HttpClients.createDefault().execute(new HttpGet(url)).getStatusLine().getStatusCode() > -100)
                    break;
            } catch (HttpHostConnectException ex) {
            }
        }
    }

    private static final int ADMIN_PORT = 48081;
    private static final int PORT = 48080;
    private static final String LOCALHOST = "127.0.0.1";
    private static final String METRICS_URL = String.format("http://%s:%d/metrics", LOCALHOST, ADMIN_PORT);
    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();
    private static final String HEALTHCHECK = String.format("http://%s:%d/healthcheck", LOCALHOST, ADMIN_PORT);

}
