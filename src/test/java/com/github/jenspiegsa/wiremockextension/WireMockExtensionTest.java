package com.github.jenspiegsa.wiremockextension;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.unmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.core.Options;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

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
			final SampleClient sampleClient = new SampleClient("http://localhost:8080/");
			assertThat(sampleClient.isOk()).isTrue();
			sampleClient.close();
		}
	}

	@Nested
	class CustomConfiguration {

		@ConfigureWireMock
		private Options options = wireMockConfig().dynamicPort();

		@InjectServer
		private WireMockServer serverMock;

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

			final SampleClient client = new SampleClient("http://localhost:" + port + "/");
			log.info(() -> "target: " + client.target.getUri());
			assertThat(client.isOk()).isTrue();
			client.close();
		}
	}

	@Nested
	@DisplayName("Test instance preparation")
	class TestInstancePreparation {

		@Test
		@DisplayName("should raise ExtensionConfigurationException when multiple configs are present.")
		void shouldRaiseExceptionWhenMultipleConfigsArePresent() {

			// given
			final Class<?> testClass = InvalidMultipleOptionsTestCase.class;

			// when
			final TestResults results = launchTests(testClass);

			// then
			then(results.getSummary().getTestsFailedCount())
					.describedAs("test instance preparation expected to fail")
					.isEqualTo(1L);
			then(results.getThrowables()).hasSize(1);
			then(results.getThrowables().get(0))
					.isInstanceOf(ExtensionConfigurationException.class)
					.hasMessageContaining("@ConfigureWireMock");
		}
		
		@Test
		@DisplayName("should raise VerificationException on unmatched request when failOnUnmatchedRequest is true.")
		void shouldRaiseExceptionOnUnmatchedRequestIfConfiguredSo() {

			// given
			final Class<?> testClass = FailOnUnmatchedRequestsTestCase.class;

			// when
			final TestResults results = launchTests(testClass);

			then(results.getSummary().getTestsFailedCount())
					.describedAs("test execution expected to fail")
					.isEqualTo(1L);
			then(results.getThrowables()).hasSize(1);
			then(results.getThrowables().get(0))
					.isInstanceOf(VerificationException.class)
					.hasMessageContaining("unmatched");
		}

		@Test
		@DisplayName("should raise VerificationException on near missed request when failOnUnmatchedRequest is true.")
		void shouldRaiseExceptionOnNearMissedRequestIfConfiguredSo() {

			// given
			final Class<?> testClass = FailOnNearMissedRequestsTestCase.class;

			// when
			final TestResults results = launchTests(testClass);

			then(results.getSummary().getTestsFailedCount())
					.describedAs("test execution expected to fail")
					.isEqualTo(1L);
			then(results.getThrowables()).hasSize(1);
			then(results.getThrowables().get(0))
					.isInstanceOf(VerificationException.class)
					.hasMessageContaining("unmatched")
					.hasMessageContaining("Closest");
		}

		@Test
		@DisplayName("should tolerate unmatched request when failOnUnmatchedRequest is false.")
		void shouldTolerateUnmatchedRequestIfConfiguredSo() {

			// given
			final Class<?> testClass = TolerateUnmatchedRequestsTestCase.class;

			// when
			final TestResults results = launchTests(testClass);

			then(results.getSummary().getTestsFailedCount())
					.describedAs("test execution expected not to fail")
					.isEqualTo(0L);
			then(results.getThrowables()).isEmpty();
		}

		private TestResults launchTests(final Class<?> testClass) {
			final Launcher launcher = LauncherFactory.create();
			final TestResults results = new TestResults();
			launcher.execute(request().selectors(selectClass(testClass)).build(), results);
			return results;
		}
	}

	@Nested
	@DisplayName("Multiple servers")
	class MultipleServers {

		@Managed WireMockServer s1 = with(wireMockConfig().dynamicPort());
		@Managed WireMockServer s2 = with(wireMockConfig().dynamicPort());
		@Managed WireMockServer s3 = with(wireMockConfig().dynamicPort());

		@Test
		@DisplayName("should work.")
		void shouldWork() {
			s1.stubFor(get("/a").willReturn(ok()));
			s2.stubFor(get("/b").willReturn(notFound()));
			s3.stubFor(get("/c").willReturn(ok()));
			then(new SampleClient("http://localhost:" + s1.port() + "/a").isOk()).isTrue();
			then(new SampleClient("http://localhost:" + s2.port() + "/b").isOk()).isFalse();
			then(new SampleClient("http://localhost:" + s3.port() + "/c").isOk()).isTrue();
		}
	}

	static class InvalidMultipleOptionsTestCase extends TestBase {
		@ConfigureWireMock Options o1 = wireMockConfig().dynamicPort();
		@ConfigureWireMock Options o2 = wireMockConfig().dynamicPort();
	}

	@WireMockSettings(failOnUnmatchedRequests = true)
	static class FailOnUnmatchedRequestsTestCase extends TestBase {

		@InjectServer WireMockServer server;
		@ConfigureWireMock Options options = wireMockConfig().dynamicPort();

		@Test
		@SuppressWarnings("JUnitTestMethodWithNoAssertions")
		void shouldFailWhenUnmatchedRequestOccurs() {
			new SampleClient("http://localhost:" + server.port() + "/").isOk();
		}
	}

	@WireMockSettings(failOnUnmatchedRequests = true)
	static class FailOnNearMissedRequestsTestCase extends TestBase {

		@InjectServer WireMockServer server;
		@ConfigureWireMock Options options = wireMockConfig().dynamicPort();

		@Test
		@SuppressWarnings("JUnitTestMethodWithNoAssertions")
		void shouldFailWhenNearMissedRequestOccurs() {
			givenThat(get("/a").willReturn(ok()));
			new SampleClient("http://localhost:" + server.port() + "/").isOk();
		}
	}

	@WireMockSettings(failOnUnmatchedRequests = false)
	static class TolerateUnmatchedRequestsTestCase extends TestBase {

		@InjectServer WireMockServer server;
		@ConfigureWireMock Options options = wireMockConfig().dynamicPort();

		@Test
		@SuppressWarnings("JUnitTestMethodWithNoAssertions")
		void shouldIgnoreUnmatchedRequests() {
			new SampleClient("http://localhost:" + server.port() + "/").isOk();
		}
	}

	@ExtendWith(WireMockExtension.class)
	private static abstract class TestBase {
		@Test
		@SuppressWarnings("JUnitTestMethodWithNoAssertions")
		void testNothing() {
		}
	}

	private static class TestResults extends SummaryGeneratingListener {

		private final List<Throwable> throwables = new ArrayList<>();

		@Override
		public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult) {
			super.executionFinished(testIdentifier, testExecutionResult);
			testExecutionResult.getThrowable().ifPresent(throwables::add);
		}

		public List<Throwable> getThrowables() {
			return unmodifiableList(throwables);
		}
	}
}
