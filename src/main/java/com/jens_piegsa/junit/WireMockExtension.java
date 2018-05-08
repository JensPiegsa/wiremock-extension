package com.jens_piegsa.junit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * @author Jens Piegsa
 */
public class WireMockExtension implements TestInstancePostProcessor, BeforeEachCallback, AfterEachCallback {

	private static final Logger log = Logger.getLogger(WireMockExtension.class.getSimpleName());

	private boolean failOnUnmatchedRequests;
	private WireMockServer server;

	// This constructor is invoked by JUnit Jupiter via reflection
	@SuppressWarnings("unused")
	private WireMockExtension() {
		this(true);
	}

	public WireMockExtension(final boolean failOnUnmatchedRequests) {
		this.failOnUnmatchedRequests = failOnUnmatchedRequests;
	}

	@Override
	public void beforeEach(final ExtensionContext context) throws IllegalAccessException {
		final Optional<WireMockSettings> wireMockSettings = retrieveAnnotationFromTestClasses(context);
		failOnUnmatchedRequests = wireMockSettings
				.map(WireMockSettings::failOnUnmatchedRequests)
				.orElse(failOnUnmatchedRequests);

		final Object testInstance = context.getRequiredTestInstance();
		final Class<?> testClass = context.getRequiredTestClass();
		final Field[] declaredFields = testClass.getDeclaredFields();

		Options options = null;
		for (final Field field : declaredFields) {
			if (field.isAnnotationPresent(ConfigureWireMock.class)) {
				if (options == null) {
					options = (Options) field.get(testInstance);
				} else {
					throw new IllegalStateException("@ConfigureWireMock only valid once per class.");
				}
			}
		}
		if (options == null) {
			options = wireMockConfig();
		}

		server = new WireMockServer(options);
		server.start();
		WireMock.configureFor("localhost", server.port());

		for (final Field field : declaredFields) {
			if (field.isAnnotationPresent(InjectServer.class)) {
				field.set(testInstance, server);
			}
		}
	}

	@Override
	public void afterEach(final ExtensionContext context) {
		checkForUnmatchedRequests();
		server.stop();
	}

	private void checkForUnmatchedRequests() {
		if (failOnUnmatchedRequests) {
			final List<LoggedRequest> unmatchedRequests = server.findAllUnmatchedRequests();
			if (!unmatchedRequests.isEmpty()) {
				final List<NearMiss> nearMisses = server.findNearMissesForAllUnmatchedRequests();
				if (nearMisses.isEmpty()) {
					throw VerificationException.forUnmatchedRequests(unmatchedRequests);
				} else {
					throw VerificationException.forUnmatchedNearMisses(nearMisses);
				}
			}
		}
	}

	@Override
	public void postProcessTestInstance(final Object testInstance, final ExtensionContext context) {
		log.info(() -> "postProcessTestInstance testInstance: " + testInstance);
	}

	private Optional<WireMockSettings> retrieveAnnotationFromTestClasses(final ExtensionContext context) {

		ExtensionContext currentContext = context;
		Optional<WireMockSettings> annotation;

		do {
			annotation = findAnnotation(currentContext.getElement(), WireMockSettings.class);

			if (!currentContext.getParent().isPresent()) {
				break;
			}

			currentContext = currentContext.getParent().get();
		} while (!annotation.isPresent() && currentContext != context.getRoot());

		return annotation;
	}
}
