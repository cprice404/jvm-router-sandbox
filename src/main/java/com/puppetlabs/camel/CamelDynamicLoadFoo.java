package com.puppetlabs.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CamelDynamicLoadFoo {


    public static class MyWork {
        private int id;

        public MyWork(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    public static class MyConsumer {

        private final String name;

        public MyConsumer(String name) {
            this.name = name;
        }
        @Handler
        public void doStuff(MyWork work) {
            System.out.println("Consumer '" + name + "' got message: " + work.getId());
        }
    }

    public static void main(String[] args) throws Exception {
        SimpleRegistry r = new SimpleRegistry();
        r.put("x", new MyConsumer("x"));
        r.put("y", new MyConsumer("y"));
        r.put("z", new MyConsumer("z"));
        final CamelContext cc = new DefaultCamelContext(r);

        RouteBuilder rb1 = buildRoute("route1", "bean:x", "bean:y");
        final RouteBuilder rb2 = buildRoute("route2", "bean:x", "bean:y", "bean:z");

        cc.addRoutes(rb1);
        cc.start();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable reRouter = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    cc.stopRoute("route1");
                    cc.removeRoute("route1");
                    cc.addRoutes(rb2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Runnable producer = new Runnable() {
            @Override
            public void run() {
                ProducerTemplate template = cc.createProducerTemplate();
                for (int i = 0; i < 100; i++) {
                    template.sendBody("direct:start", new MyWork(i));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        executor.execute(reRouter);
        executor.execute(producer);

        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}

        cc.stop();
    }

    private static RouteBuilder buildRoute(final String routeId, final String... uris) {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start").routeId(routeId).
                        loadBalance().
                        roundRobin().
                        to(uris);
            }
        };
    }
}
