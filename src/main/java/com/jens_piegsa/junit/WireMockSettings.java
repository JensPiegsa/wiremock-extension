package com.jens_piegsa.junit;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Jens Piegsa
 */
@ExtendWith(WireMockExtension.class)
@Retention(RUNTIME)
public @interface WireMockSettings {

	boolean failOnUnmatchedRequests() default true;

}
