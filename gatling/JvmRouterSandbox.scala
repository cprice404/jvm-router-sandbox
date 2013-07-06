package com.puppetlabs.benchmarks 
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._
import assertions._

class JvmRouterSandbox extends Simulation {

	val httpConf = httpConfig
			.baseURL("http://localhost:4000")
			.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
			.acceptEncodingHeader("gzip, deflate")
			.acceptLanguageHeader("en-US,en;q=0.5")
			.connection("keep-alive")
			.userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:20.0) Gecko/20100101 Firefox/20.0")


	val scn = scenario("Scenario Name").repeat(5) {
		exec(http("proxied-root")
					.get("/")
			)
		.pause(1)
		.exec(http("proxied-root")
					.get("/")
			)
		.pause(1)
		.exec(http("local")
					.get("/local1")
			)
		.pause(1)
		.exec(http("proxied-nomatch")
					.get("/local2")
			)
		.pause(1)
		.exec(http("local")
					.get("/local1")
			)
		.pause(1)
		.exec(http("proxied-nomatch")
					.get("/local2")
			)
		.pause(1)
		.exec(http("proxied-root")
					.get("/")
			)
		.pause(1)
		.exec(http("proxied-root")
					.get("/")
			)
    .pause(1)
    .exec(http("local")
      .get("/local1")
    )
    .pause(1)
    .exec(http("proxied-nomatch")
      .get("/local2")
    )
  }

	setUp(scn.users(10000).ramp(30).protocolConfig(httpConf))
}