Dropwizard Websocket Support
==========
[![Build Status](https://api.travis-ci.org/LivePersonInc/dropwizard-websockets.svg?branch=master)](https://travis-ci.org/LivePersonInc/dropwizard-websockets)
[![Maven Central](https://img.shields.io/maven-central/v/com.liveperson/dropwizard-websockets.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22dropwizard-websockets%22)

A [3rd party Dropwizard bundle](http://modules.dropwizard.io/thirdparty/), that enhances [Dropwizard](http://www.dropwizard.io) capablities to support not only JAX-RS resources but also websockets endpoints using the JSR-356 API.

The websockets endpoints will be instrumented the same way Dropwizards does with JAX-RS resources, and their metrics will be exposed in the same way. This includes:

* Counters of current open sessions.
* Counters and rate meters for new connections.
* Counters and rate meters for messeages revieved by the endpoint.
* Timers and statstics for session duration.

Maven Dependency
---
Add the maven dependency: 

```xml
<dependency>
  <groupId>com.liveperson</groupId>
  <artifactId>dropwizard-websocket</artifactId>
  <version></version>
</dependency>
```

Usage
---
In your code you should add the ``WebsocketBundle`` in the initialization stage of the Application. Give the bundle your enpoints classes (or ``ServerEndpoindConfig`` in case of programmatic endpoints) as parameteres:

```java
public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(new WebsocketBundle(MyWebSocket1.class, MyWebSocket2.class));
}
```

That's all.
A full example can be found in the [tests classes](https://github.com/LivePersonInc/dropwizard-websockets/blob/master/src/test/java/io/dropwizard/websockets/MyApp.java).

Metrics
---
In order to collect metrics on your endpoints, you should annotate them with metrics annotations:

```java
@Metered
@Timed
@ExceptionMetered
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
```

Then you'll be able to see your metrics as follows:

```json
{
  "counters" : {
    "io.dropwizard.websockets.MyApp$AnnotatedEchoServer.openConnections" : {
      "count" : 2
    }
  },
  "meters" : {
    "io.dropwizard.websockets.MyApp$AnnotatedEchoServer.OnError" : {
      "count" : 0,
      "m15_rate" : 0.0,
      "m1_rate" : 0.0,
      "m5_rate" : 0.0,
      "mean_rate" : 0.0,
      "units" : "events/second"
    },
    "io.dropwizard.websockets.MyApp$AnnotatedEchoServer.OnMessage" : {
      "count" : 3,
      "m15_rate" : 0.6,
      "m1_rate" : 0.6,
      "m5_rate" : 0.6,
      "mean_rate" : 0.3194501069682357,
      "units" : "events/second"
    }
  },
  "timers" : {
    "io.dropwizard.websockets.MyApp$AnnotatedEchoServer" : {
      "count" : 1,
      "max" : 0.101819137,
      "mean" : 0.101819137,
      "min" : 0.101819137,
      "p50" : 0.101819137,
      "p75" : 0.101819137,
      "p95" : 0.101819137,
      "p98" : 0.101819137,
      "p99" : 0.101819137,
      "p999" : 0.101819137,
      "stddev" : 0.0,
      "m15_rate" : 0.2,
      "m1_rate" : 0.2,
      "m5_rate" : 0.2,
      "mean_rate" : 0.10647618704871187,
      "duration_units" : "seconds",
      "rate_units" : "calls/second"
    }
  }
}
```
Alternatives
---
See also [dropwizard-websocket-jee7-bundle](https://github.com/TomCools/dropwizard-websocket-jee7-bundle).
