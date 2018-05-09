package com.github.jenspiegsa.mockitoextension;

import static com.github.jenspiegsa.mockitoextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import com.github.jenspiegsa.mockitoextension.ConfigureWireMock;
import com.github.jenspiegsa.mockitoextension.InjectServer;
import com.github.jenspiegsa.mockitoextension.SampleClient;
import com.github.jenspiegsa.mockitoextension.WireMockExtension;
import com.github.jenspiegsa.mockitoextension.WireMockSettings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Jens Piegsa
 */
@DisplayName("WireMockExtension usage")
@ExtendWith(WireMockExtension.class)
class ExampleTest {

	@Nested
	@DisplayName("1. Simple example")
	class SimpleExample {

		SampleClient sampleClient;

		@BeforeEach
		void setup() {
			sampleClient = new SampleClient("http://localhost:8080/");
		}

		@AfterEach
		void afterEach() {
			sampleClient.close();
		}

		@Nested
		@DisplayName("isOk()")
		class IsOk {

			@Test
			@DisplayName("should return true when status ok (200).")
			void shouldReturnTrueWhenStatusOk() {

				givenThat(get("/").willReturn(ok()));

				// when
				final boolean clientOk = sampleClient.isOk();

				then(clientOk).isTrue();
			}

			@Test
			@DisplayName("should return false when status not found (404).")
			void shouldReturnFalseWhenStatusNotFound() {

				givenThat(get("/").willReturn(notFound()));

				// when
				final boolean clientOk = sampleClient.isOk();

				then(clientOk).isFalse();
			}
		}
	}

	@Nested
	@DisplayName("2. Customized example")
	@WireMockSettings(failOnUnmatchedRequests = true)
	class CustomExample {

		@InjectServer
		WireMockServer serverMock;

		@ConfigureWireMock
		Options options = wireMockConfig()
				.dynamicPort()
				.notifier(new ConsoleNotifier(true));

		SampleClient sampleClient;

		@BeforeEach
		void setup() {
			sampleClient = new SampleClient("http://localhost:" + serverMock.port() + "/");
		}

		@ParameterizedTest(name = "should return {1} when status {0}")
		@DisplayName("isOk()")
		@CsvSource({"200, true", "404, false", "503, false"})
		void shouldReturnStatusOk(final int status, final boolean expectedOutcome) {

			givenThat(get("/").willReturn(aResponse().withStatus(status)));

			// when
			final boolean actualOutcome = sampleClient.isOk();

			then(actualOutcome).isEqualTo(expectedOutcome);
		}

		@AfterEach
		void afterEach() {
			sampleClient.close();
		}
	}

	@Nested
	@DisplayName("3. Example with multiple servers")
	class ExampleWithMultipleServers {

		@Managed WireMockServer s1 = with(wireMockConfig().dynamicPort());
		@Managed WireMockServer s2 = with(wireMockConfig().dynamicPort());
		@Managed WireMockServer s3 = with(wireMockConfig().dynamicPort());
		@Managed WireMockServer s4 = with(wireMockConfig().dynamicPort()).failOnUnmagedRequest(false);

		@Test
		@DisplayName("should work.")
		void shouldWork() {
			s1.stubFor(get("/a").willReturn(ok()));
			s2.stubFor(get("/b").willReturn(notFound()));
			s3.stubFor(get("/c").willReturn(ok()));
			then(new SampleClient("http://localhost:" + s1.port() + "/a").isOk()).isTrue();
			then(new SampleClient("http://localhost:" + s2.port() + "/b").isOk()).isFalse();
			then(new SampleClient("http://localhost:" + s3.port() + "/c").isOk()).isTrue();
			then(new SampleClient("http://localhost:" + s4.port() + "/d").isOk()).isFalse();
		}
	}
}
