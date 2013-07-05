package com.puppetlabs.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

public class CamelLoadFoo {

    public static class MyConsumer {

        private final String name;

        public MyConsumer(String name) {
            this.name = name;
        }
        @Handler
        public void doStuff(String msg) {
            System.out.println("Consumer '" + name + "' got message: " + msg);
        }
    }

    public static void main(String[] args) throws Exception {
        SimpleRegistry r = new SimpleRegistry();
        r.put("x", new MyConsumer("x"));
        r.put("y", new MyConsumer("y"));
        r.put("z", new MyConsumer("z"));
        CamelContext cc = new DefaultCamelContext(r);
        cc.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:start").
                        loadBalance().
                        roundRobin().
                        to("bean:x", "bean:y", "bean:z");
            }
        });
        ProducerTemplate template = cc.createProducerTemplate();
        cc.start();
        for (int i = 0; i < 100; i++) {
            template.sendBody("direct:start", "msg" + i);
        }
        cc.stop();
    }
}
