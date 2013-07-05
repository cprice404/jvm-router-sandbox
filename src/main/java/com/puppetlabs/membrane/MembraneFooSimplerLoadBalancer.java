package com.puppetlabs.membrane;

import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.interceptor.balancer.*;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

public class MembraneFooSimplerLoadBalancer {

    public static void main(String[] args) throws Exception {

        String balancerName = "HelloWorldBalancer";
        String clusterName = Cluster.DEFAULT_NAME;

        HttpRouter router = new HttpRouter();
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
    }
}
