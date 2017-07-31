package com.redhat.che.keycloak.shared;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;

public class ApplicationKeycloakConfigResolver extends AbstractKeycloakConfigResolver {

    @Override
    protected AdapterConfig prepareConfig() {
        AdapterConfig config = new AdapterConfig();
        config.setSslRequired(SslRequired.EXTERNAL.toString().toLowerCase());
        config.setCors(true);
        config.setBearerOnly(false);
        config.setPublicClient(true);
        config.setConnectionPoolSize(20);
        config.setDisableTrustManager(true);
        return config;
    }
}
