package com.github.jenspiegsa.wiremockextension;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tomakehurst.wiremock.WireMockServer;

@ExtendWith(WireMockExtension.class)
class MultiLevelTest extends MultiLevelParent {

    @Managed
    private WireMockServer child = with(options().dynamicPort());

    @Test
    void nestedTest() {
        assertThat(child).isNotNull();
        assertThat(child.isRunning()).describedAs("child class server is expected to be running.").isTrue();

        assertThat(parent).isNotNull();
        assertThat(parent.isRunning()).describedAs("parent class server is expected to be running.").isTrue();
    }
}
