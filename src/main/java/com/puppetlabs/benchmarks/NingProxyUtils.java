package com.puppetlabs.benchmarks;

import com.google.common.io.ByteStreams;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.generators.InputStreamBodyGenerator;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

public class NingProxyUtils
{
    protected static HashSet<String> dontProxyHeaders = new HashSet<String>();

    {
        dontProxyHeaders.add("proxy-connection");
        dontProxyHeaders.add("connection");
        dontProxyHeaders.add("keep-alive");
        dontProxyHeaders.add("transfer-encoding");
        dontProxyHeaders.add("te");
        dontProxyHeaders.add("trailer");
        dontProxyHeaders.add("proxy-authorization");
        dontProxyHeaders.add("proxy-authenticate");
        dontProxyHeaders.add("upgrade");
    }

    public static void proxyServletRequest(final AsyncHttpClient client,
                                    final String host,
                                    final int port,
                                    final ServletRequest req,
                                    ServletResponse res)
            throws IOException
    {
        while (res instanceof HttpServletResponseWrapper) {
            res = ((HttpServletResponseWrapper) res).getResponse();
        }
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        final RequestBuilder builder = cloneRequest(request, host, port);

        try {
            final com.ning.http.client.Response proxiedResponse = client.executeRequest(builder.build()).get();

            response.setStatus(proxiedResponse.getStatusCode());
            // Copy headers
            for (final String headerName : proxiedResponse.getHeaders().keySet()) {
                if (dontProxyHeaders.contains(headerName)) {
                    continue;
                }

                for (final String headerValue : proxiedResponse.getHeaders().get(headerName)) {
                    response.addHeader(headerName, headerValue);
                }
            }
            // Copy response body
            final ServletOutputStream responseOutputStream = response.getOutputStream();
            final InputStream stream = proxiedResponse.getResponseBodyAsStream();
            ByteStreams.copy(stream, responseOutputStream);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    private static RequestBuilder cloneRequest(final HttpServletRequest request,
                                        final String host,
                                        final int port) throws IOException
    {
        final RequestBuilder builder = new RequestBuilder();
        boolean hasContent = false;
        boolean hasXff = false;

        String connectionHdr = request.getHeader("Connection");
        if (connectionHdr != null) {
            connectionHdr = connectionHdr.toLowerCase();
            if (!connectionHdr.contains("keep-alive") && !connectionHdr.contains("close")) {
                connectionHdr = null;
            }
        }

        // We are guaranteed that headers are Strings
        @SuppressWarnings("unchecked")
        final Collection headerNames = Collections.list(request.getHeaderNames());
        for (final Object headerObjectName : headerNames) {
            final String headerName = (String) headerObjectName;

            // Don't copy headers on close
            if (connectionHdr != null && connectionHdr.contains(headerName)) {
                continue;
            }

            if (dontProxyHeaders.contains(headerName)) {
                continue;
            }

            if ("content-type".equalsIgnoreCase(headerName)) {
                hasContent = true;
            }
            if ("X-Forwarded-For".equalsIgnoreCase(headerName)) {
                hasXff = true;
            }

            @SuppressWarnings("unchecked")
            final Collection headerValues = Collections.list(request.getHeaders(headerName));
            for (final Object headerValue : headerValues) {
                builder.addHeader(headerName, (String) headerValue);
            }
        }
        builder.addHeader("Via", "Ning proxy");
        if (!hasXff) {
            builder.addHeader("X-Forwarded-For", request.getRemoteAddr());
        }

        // Need to set the Method before setting the body
        builder.setMethod(request.getMethod());
        if (hasContent) {
            final InputStream in = request.getInputStream();
            builder.setBody(new InputStreamBodyGenerator(in));
        }

        String uri = "http://" + host + ":" + port + request.getRequestURI();
        if (request.getQueryString() != null) {
            uri += "?" + request.getQueryString();
        }
        builder.setUrl(uri);

        return builder;
    }
}