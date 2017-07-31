package com.redhat.che.keycloak.shared;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;

public class ServicesKeycloakConfigResolver extends AbstractKeycloakConfigResolver {

    @Override
    protected AdapterConfig prepareConfig() {
        AdapterConfig config = new AdapterConfig();
        config.setSslRequired(SslRequired.EXTERNAL.toString().toLowerCase());
        config.setCors(true);
        config.setBearerOnly(true);
        config.setPublicClient(false);
        config.setConnectionPoolSize(20);
        config.setDisableTrustManager(true);
        config.setEnableBasicAuth(false);
        config.setEnableBasicAuth(false);
        config.setExposeToken(true);
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("secret", "08a8bcd1-f362-446a-9d2b-d34b8d464185");
        config.setCredentials(credentials);
        return config;
    }
    
}
