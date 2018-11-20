package com.github.jenspiegsa.wiremockextension;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;

class MultiLevelParent {

    @Managed
    final WireMockServer parent = with(options().dynamicPort());
}
