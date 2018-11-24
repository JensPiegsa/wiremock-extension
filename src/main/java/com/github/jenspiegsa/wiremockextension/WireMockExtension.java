package com.github.jenspiegsa.wiremockextension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;

/**
 * @author Jens Piegsa
 */
public class WireMockExtension implements BeforeEachCallback, AfterEachCallback, TestInstancePostProcessor {

	private boolean generalFailOnUnmatchedRequests;

	/**
	 * {@link ExtensionContext.Namespace} in which WireMockServers are stored,
	 * keyed by test class.
	 */
	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(WireMockExtension.class);

	// This constructor is invoked by JUnit via reflection
	@SuppressWarnings("unused")
	private WireMockExtension() {
		this(true);
	}

	public WireMockExtension(final boolean failOnUnmatchedRequests) {
		generalFailOnUnmatchedRequests = failOnUnmatchedRequests;
	}

	@Override
	public void postProcessTestInstance(final Object testInstance, final ExtensionContext context) throws Exception {

		final List<WireMockServer> managedServers = retrieveAnnotatedFields(context, Managed.class, WireMockServer.class).stream()
				.map(field -> ReflectionUtils.readFieldValue(field, testInstance))
				.map(Optional::get)
				.map(WireMockServer.class::cast)
				.map(Objects::requireNonNull)
				.collect(toList());

		if (managedServers.isEmpty()) {
			Options options = null;
			for (final Field field : retrieveAnnotatedFields(context, ConfigureWireMock.class, Options.class)) {
				if (options == null) {
					options = (Options) makeAccessible(field).get(testInstance);
				} else {
					throw new ExtensionConfigurationException("@ConfigureWireMock only valid once per class.");
				}
			}
			if (options == null) {
				options = wireMockConfig();
			}

			final List<Field> injectedServerFields = retrieveAnnotatedFields(context, InjectServer.class, WireMockServer.class);
			if (!injectedServerFields.isEmpty()) {
				final WireMockServer server = new WireMockServer(options);
				for (final Field field : injectedServerFields) {
					makeAccessible(field).set(testInstance, server);
				}
				context.getStore(NAMESPACE).put(testInstance.getClass(), singletonList(server));
			}
		} else {
			context.getStore(NAMESPACE).put(testInstance.getClass(), managedServers);
		}
	}

	@Override
	public void beforeEach(final ExtensionContext context) {

		final Optional<WireMockSettings> wireMockSettings = retrieveAnnotation(context, WireMockSettings.class);
		generalFailOnUnmatchedRequests = wireMockSettings
				.map(WireMockSettings::failOnUnmatchedRequests)
				.orElse(generalFailOnUnmatchedRequests);

		final List<WireMockServer> wireMockServers = collectServers(context);
		if (wireMockServers.isEmpty()) {
			// Simple case
			final WireMockServer server = new WireMockServer();
			context.getStore(NAMESPACE).put(context.getRequiredTestClass(), singletonList(server));
			startServer(server);
		} else {
			wireMockServers.forEach(WireMockExtension::startServer);
		}
	}

	@Override
	public void afterEach(final ExtensionContext context) {
		final List<WireMockServer> wireMockServers = collectServers(context);
		// Stopping all servers first
		wireMockServers.forEach(WireMockExtension::stopServer);
		wireMockServers.forEach(this::checkForUnmatchedRequests);
	}

	private void checkForUnmatchedRequests(final WireMockServer server) {
		
		final boolean mustCheck = Optional.of(server)
			.filter(ManagedWireMockServer.class::isInstance)
			.map(ManagedWireMockServer.class::cast)
			.map(ManagedWireMockServer::failOnUnmatchedRequests)
			.orElse(generalFailOnUnmatchedRequests);

		if (mustCheck) {
			final List<LoggedRequest> unmatchedRequests = server.findAllUnmatchedRequests();
			if (!unmatchedRequests.isEmpty()) {
				final List<NearMiss> nearMisses = server.findNearMissesForAllUnmatchedRequests();
				throw nearMisses.isEmpty()
					  ? VerificationException.forUnmatchedRequests(unmatchedRequests)
					  : VerificationException.forUnmatchedNearMisses(nearMisses);
			}
		}
	}

	private static <A extends Annotation> Optional<A> retrieveAnnotation(final ExtensionContext context,
	                                                                     final Class<A> annotationType) {

		Optional<ExtensionContext> currentContext = Optional.of(context);
		Optional<A> annotation = Optional.empty();

		while (currentContext.isPresent() && !annotation.isPresent()) {
			annotation = AnnotationSupport.findAnnotation(currentContext.get().getElement(), annotationType);
			currentContext = currentContext.get().getParent();
		}
		return annotation;
	}

	private static List<Field> retrieveAnnotatedFields(final ExtensionContext context,
	                                                   final Class<? extends Annotation> annotationType,
	                                                   final Class<?> fieldType) {

		return context.getElement()
				.filter(Class.class::isInstance)
				.map(Class.class::cast)
				.map(testInstanceClass ->
						AnnotationUtils.findAnnotatedFields(testInstanceClass, annotationType, field -> fieldType.isAssignableFrom(field.getType()))
				)
				.orElseGet(Collections::emptyList);
	}

	private static void startServer(final WireMockServer server) {
		if (!server.isRunning()) {
			server.start();
			WireMock.configureFor("localhost", server.port());
		}
	}

	private static void stopServer(final WireMockServer server) {
		server.stop();
	}

	private static List<WireMockServer> collectServers(final ExtensionContext context) {

		return collectTestClasses(context)
				.map(testClass -> context.getStore(NAMESPACE).get(testClass))
				.filter(Objects::nonNull)
				.map(List.class::cast)
				.flatMap(Collection::stream)
				.map(WireMockServer.class::cast)
				.collect(toList());
	}

	private static Stream<Class<?>> collectTestClasses(final ExtensionContext context) {

		return Stream.concat(
				Stream.of(context.getRequiredTestClass()),
				context.getParent()
						.filter(parentContext -> parentContext != context.getRoot())
						.map(WireMockExtension::collectTestClasses)
						.orElseGet(Stream::empty)
		).distinct();
	}
}
