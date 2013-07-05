package com.puppetlabs.benchmarks;

import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.config.Path;
import com.predic8.membrane.core.interceptor.balancer.Balancer;
import com.predic8.membrane.core.interceptor.balancer.Cluster;
import com.predic8.membrane.core.interceptor.balancer.LoadBalancingInterceptor;
import com.predic8.membrane.core.interceptor.balancer.RoundRobinStrategy;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BenchmarkMembraneRouter {
    public static void main(String[] args) throws Exception {

        Server jetty = new Server(8080);

        jetty.setHandler(new AbstractHandler() {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                request.setHandled(true);
                response.getWriter().println("Hello from local service");
            }
        });
        jetty.start();

        String balancerName = "HelloWorldBalancer";
        String clusterName = Cluster.DEFAULT_NAME;

        HttpRouter router = new HttpRouter();
        ServiceProxy localService1 = new ServiceProxy();
        localService1.setPort(4000);
        localService1.setPath(new Path(false, "/local1"));
        localService1.setTargetHost("localhost");
        localService1.setTargetPort(8080);
        router.getRuleManager().addProxyAndOpenPortIfNew(localService1);

        ServiceProxy sp = new ServiceProxy();
        sp.setKey(new ServiceProxyKey(4000));
        LoadBalancingInterceptor balancingInterceptor = new LoadBalancingInterceptor();
        balancingInterceptor.setName(balancerName);
        balancingInterceptor.setDispatchingStrategy(new RoundRobinStrategy());
        sp.getInterceptors().add(balancingInterceptor);
        router.getRuleManager().addProxyAndOpenPortIfNew(sp);
        router.init();

        Balancer balancer = balancingInterceptor.getClusterManager();
        balancer.up(clusterName, "localhost", 5000);
        balancer.up(clusterName, "localhost", 6000);

        jetty.join();
    }


}
