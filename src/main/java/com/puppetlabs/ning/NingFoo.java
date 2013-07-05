package com.puppetlabs.ning;


import com.ning.http.client.*;

import java.util.concurrent.Future;

public class NingFoo {
    public static void main(String[] args) throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        Future<Integer> f = c.prepareGet("https://espn.go.com/nba").execute(new AsyncHandler<Integer>() {

            private int statusCode;

            @Override
            public void onThrowable(Throwable throwable) {
                System.out.println("THROWABLE!" + throwable);
                throw new RuntimeException(throwable);
            }

            @Override
            public STATE onBodyPartReceived(HttpResponseBodyPart httpResponseBodyPart) throws Exception {
                System.out.println("Body part receieved");
                httpResponseBodyPart.writeTo(System.out);
                return STATE.CONTINUE;
            }

            @Override
            public STATE onStatusReceived(HttpResponseStatus httpResponseStatus) throws Exception {
                System.out.println("Status receieved: " + httpResponseStatus.getStatusText());
                statusCode = httpResponseStatus.getStatusCode();
                return STATE.CONTINUE;
            }

            @Override
            public STATE onHeadersReceived(HttpResponseHeaders httpResponseHeaders) throws Exception {
                System.out.println("Headers received: " + httpResponseHeaders.getHeaders());
                return STATE.CONTINUE;
            }

            @Override
            public Integer onCompleted() throws Exception {
                System.out.println("COMPLETED");

                return statusCode;
            }
        });

        System.out.println("Waiting for future");
        System.out.println("Response code: " + f.get());
    }
}
