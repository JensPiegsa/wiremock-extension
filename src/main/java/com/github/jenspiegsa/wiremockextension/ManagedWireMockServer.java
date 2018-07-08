package com.github.jenspiegsa.wiremockextension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;

/**
 * @author Jens Piegsa
 */
public class ManagedWireMockServer extends WireMockServer {

	private Boolean failOnUnmatchedRequests;

	public ManagedWireMockServer() {
		this(wireMockConfig());
	}

	public ManagedWireMockServer(final int port) {
		this(wireMockConfig().port(port));
	}

	public ManagedWireMockServer(final int port, final Integer httpsPort) {
		this(wireMockConfig().port(port).httpsPort(httpsPort));
	}

	public ManagedWireMockServer(final Options options) {
		super(options);
	}

	public static ManagedWireMockServer with(final Options options) {
		return new ManagedWireMockServer(options);
	}

	public ManagedWireMockServer failOnUnmagedRequest(final boolean failOnUnmatchedRequests) {
		this.failOnUnmatchedRequests = failOnUnmatchedRequests;
		return this;
	}

	public Boolean failOnUnmatchedRequests() {
		return failOnUnmatchedRequests;
	}
}
