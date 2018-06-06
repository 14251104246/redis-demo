
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class RecordedSimulation extends Simulation {

	val httpProtocol = http
		.baseURL("http://detectportal.firefox.com")
		.inferHtmlResources()
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("zh-CN,en-US;q=0.7,en;q=0.3")
		.userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:60.0) Gecko/20100101 Firefox/60.0")

	val headers_0 = Map("Content-Type" -> "application/ocsp-request")

	val headers_3 = Map(
		"Accept" -> "*/*",
		"Pragma" -> "no-cache")

    val uri1 = "http://ocsp2.globalsign.com/gsorganizationvalsha2g2"
    val uri2 = "http://detectportal.firefox.com/success.txt"

	val scn = scenario("RecordedSimulation")
		.exec(http("request_0")
			.post(uri1 + "")
			.headers(headers_0)
			.body(RawFileBody("RecordedSimulation_0000_request.txt"))
			.resources(http("request_1")
			.post(uri1 + "")
			.headers(headers_0)
			.body(RawFileBody("RecordedSimulation_0001_request.txt"))))
		.pause(5)
		.exec(http("request_2")
			.post(uri1 + "")
			.headers(headers_0)
			.body(RawFileBody("RecordedSimulation_0002_request.txt")))
		.pause(1)
		.exec(http("request_3")
			.get("/success.txt")
			.headers(headers_3))
		.pause(1)
		.exec(http("request_4")
			.get("/success.txt")
			.headers(headers_3))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}