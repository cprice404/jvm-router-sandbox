package com.puppetlabs.membrane;

import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.interceptor.balancer.*;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

public class MembraneFooLoadBalancingInterceptor {
    public static void main(String[] args) throws Exception {;
        HttpRouter service1 = new HttpRouter();
        ServiceProxy sp1 = new ServiceProxy(new ServiceProxyKey("localhost",
                "GET", ".*", 2000), "localhost", 5000);
        service1.getRuleManager().addProxyAndOpenPortIfNew(sp1);
        service1.init();

        HttpRouter service2 = new HttpRouter();
        ServiceProxy sp2 = new ServiceProxy(new ServiceProxyKey("localhost",
                "GET", ".*", 3000), "localhost", 6000);
        service2.getRuleManager().addProxyAndOpenPortIfNew(sp2);
        service2.init();

        HttpRouter balancer = new HttpRouter();
        ServiceProxy sp3 = new ServiceProxy(new ServiceProxyKey("localhost",
                "GET", ".*", 4000), "foo.localdomain", 80);
        LoadBalancingInterceptor balancingInterceptor = new LoadBalancingInterceptor();
        balancingInterceptor.setName("Default");
        sp3.getInterceptors().add(balancingInterceptor);
        balancer.getRuleManager().addProxyAndOpenPortIfNew(sp3);
        balancer.init();

        BalancerUtil.lookupBalancer(balancer, "Default").up("Default", "localhost", 2000);
        BalancerUtil.lookupBalancer(balancer, "Default").up("Default", "localhost", 3000);

        balancingInterceptor.setDispatchingStrategy(new RoundRobinStrategy());
    }
}
