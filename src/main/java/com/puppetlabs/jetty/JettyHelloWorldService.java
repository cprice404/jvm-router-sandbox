package com.puppetlabs.jetty;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JettyHelloWorldService {

    public static void main(String[] args) throws Exception {
        final String serviceName = args[0];
        int port = Integer.valueOf(args[1]);
        Server server = new Server(port);

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                request.setHandled(true);
                response.getWriter().println("Hello from " + serviceName);
            }
        });
        server.start();
        server.join();
    }
}
