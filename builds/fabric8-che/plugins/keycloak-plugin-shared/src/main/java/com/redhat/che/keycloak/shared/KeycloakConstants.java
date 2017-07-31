package com.redhat.che.keycloak.shared;

public class KeycloakConstants {

    public static final String KEYCLOAK_SETTINGS_ENDPOINT_PATH = "/keycloak/settings";
    
    public static final String KEYCLOAK_SETTING_PREFIX = "che.keycloak.";
    public static final String DISABLED_SETTING = KEYCLOAK_SETTING_PREFIX + "disabled";
    public static final String AUTH_SERVER_URL_SETTING = KEYCLOAK_SETTING_PREFIX + "auth-server-url";
    public static final String REALM_SETTING = KEYCLOAK_SETTING_PREFIX + "realm";
    public static final String CLIENT_ID_SETTING = KEYCLOAK_SETTING_PREFIX + "client-id";
    
    public static final String getEndpoint(String apiEndpoint) {
        return apiEndpoint + KEYCLOAK_SETTINGS_ENDPOINT_PATH;
    }

}
