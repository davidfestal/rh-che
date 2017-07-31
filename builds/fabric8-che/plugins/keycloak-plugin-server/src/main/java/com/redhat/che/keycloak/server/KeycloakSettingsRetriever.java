package com.redhat.che.keycloak.server;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.che.keycloak.shared.KeycloakSettings;

@Singleton
public class KeycloakSettingsRetriever {
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakSettingsRetriever.class);
    
    private final String apiEndpoint;
    
    @Inject
    public KeycloakSettingsRetriever(@Named("che.api") String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
        LOG.info("Endpoint = " + apiEndpoint);
        KeycloakSettings.pullFromApiEndpointIfNecessary(apiEndpoint);
    }

    public Map<String, String> getSettings() {
        KeycloakSettings.pullFromApiEndpointIfNecessary(apiEndpoint);
        return KeycloakSettings.get();
    }
}
