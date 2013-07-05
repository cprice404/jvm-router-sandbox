package com.puppetlabs.benchmarks;

import com.ning.http.client.AsyncHttpClient;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkCamelNingRouter {
    public static void main(String[] args) throws Exception {

        final CamelNingProxy proxy = new CamelNingProxy(
                new CamelProxyNode("localhost", 5000),
                new CamelProxyNode("localhost", 6000));

        Server jetty = new Server(4000);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        jetty.setHandler(context);

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println("Hello from local service");
            }
        }), "/local1");

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                proxy.service(req, resp);
            }
        }), "/");

        jetty.start();
        jetty.join();
    }


    public static class ServletReqResp {
        private final HttpServletRequest req;
        private final HttpServletResponse resp;

        public ServletReqResp(HttpServletRequest req, HttpServletResponse resp) {
            this.req = req;
            this.resp = resp;
        }

        public HttpServletRequest getReq() {
            return req;
        }

        public HttpServletResponse getResp() {
            return resp;
        }
    }

    public static class CamelProxyNode {

        private String host;
        private int port;

        public CamelProxyNode(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Handler
        public void serviceRequest(ServletReqResp reqResp) throws IOException {
            AsyncHttpClient client = new AsyncHttpClient();
            NingProxyUtils.proxyServletRequest(client, getHost(), getPort(), reqResp.getReq(), reqResp.getResp());
        }
    }

    public static class CamelNingProxy {
        private CamelProxyNode[] nodes;
        private ProducerTemplate template;

        public CamelNingProxy(CamelProxyNode... nodes) throws Exception {
            this.nodes = nodes;
            SimpleRegistry r = new SimpleRegistry();
            CamelContext cc = new DefaultCamelContext(r);
            final List<Endpoint> endpoints = new ArrayList<Endpoint>();
            for (CamelProxyNode n : nodes) {
                String beanId = n.getHost() + n.getPort();
                r.put(beanId, n);
                endpoints.add(cc.getEndpoint("bean:" + beanId));
            }
            cc.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").
                    loadBalance().
                    roundRobin().
                    to(endpoints);
                }
            });
            template = cc.createProducerTemplate();
            cc.start();
        }

        public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            template.sendBody("direct:start", new ServletReqResp(req, resp));
        }
    }


}
