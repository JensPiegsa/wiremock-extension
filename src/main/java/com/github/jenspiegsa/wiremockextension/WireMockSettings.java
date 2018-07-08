package com.github.jenspiegsa.wiremockextension;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Jens Piegsa
 */
@ExtendWith(WireMockExtension.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface WireMockSettings {

	boolean failOnUnmatchedRequests() default true;

}
