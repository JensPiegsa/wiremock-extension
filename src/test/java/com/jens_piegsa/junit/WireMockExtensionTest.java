package com.jens_piegsa.junit;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Jens Piegsa
 */
@DisplayName("WireMock Extension")
@ExtendWith(WireMockExtension.class)
@WireMockSettings(failOnUnmatchedRequests = false)
class WireMockExtensionTest {

	private static final Logger log = Logger.getLogger(WireMockExtensionTest.class.getSimpleName());

	@Nested
	class DefaultConfiguration {

		static final int DEFAULT_PORT = 8080;

		@InjectServer
		WireMockServer serverMock;

		@Test
		@DisplayName("should start and inject server.")
		void shouldInjectServer() {
			assertThat(serverMock).isNotNull();
			assertThat(serverMock.isRunning()).describedAs("server expected to be running.").isTrue();
		}

		@Test
		@DisplayName("should return status ok.")
		void shouldReturnStatusOk() {
			givenThat(get("/").willReturn(ok()));
			final int port = serverMock.port();
			assertThat(port).isEqualTo(8080);
			assertThat(new ExampleClient("http://localhost:8080/").isOk()).isTrue();
		}
	}

	@Nested
	class CustomConfiguration {

		@ConfigureWireMock
		Options options = WireMockConfiguration.wireMockConfig().dynamicPort();

		@InjectServer
		WireMockServer serverMock;

		@Test
		@DisplayName("should start and inject server.")
		void shouldInjectServer() {
			assertThat(serverMock).isNotNull();
			assertThat(serverMock.isRunning()).describedAs("server expected to be running.").isTrue();
		}

		@Test
		@DisplayName("should return status ok.")
		void shouldReturnStatusOk() {
			assertThat(serverMock.isRunning()).isTrue();
			final int port = serverMock.port();
			log.info(() -> "port: " + port);

			givenThat(get("/").willReturn(ok()));

			final ExampleClient client = new ExampleClient("http://localhost:" + port + "/");
			log.info(() -> "target: " + client.target.getUri());
			assertThat(client.isOk()).isTrue();
		}
	}
}
