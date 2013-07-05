package com.puppetlabs.membrane;

import com.predic8.membrane.core.HttpRouter;
import com.predic8.membrane.core.interceptor.balancer.Cluster;
import com.predic8.membrane.core.interceptor.balancer.LoadBalancingInterceptor;
import com.predic8.membrane.core.interceptor.balancer.Node;
import com.predic8.membrane.core.interceptor.balancer.RoundRobinStrategy;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MembraneFooProxy {
    public static void main(String[] args) throws Exception {

        String hostname = "*";
        String method = "GET";
        String path = ".*";
        int listenPort = 4000;

        ServiceProxyKey key = new ServiceProxyKey(hostname, method, path, listenPort);

        ServiceProxy proxy = new ServiceProxy(key, "google.com", 80);
//        proxy.setKey(key);
//        proxy.getInterceptors().add(lbi);

        HttpRouter router = new HttpRouter();
        router.add(proxy);
        router.init();

    }
}
