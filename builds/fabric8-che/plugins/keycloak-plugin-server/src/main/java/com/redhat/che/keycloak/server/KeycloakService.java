/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.redhat.che.keycloak.server;

import static com.redhat.che.keycloak.shared.KeycloakConstants.AUTH_SERVER_URL_SETTING;
import static com.redhat.che.keycloak.shared.KeycloakConstants.CLIENT_ID_SETTING;
import static com.redhat.che.keycloak.shared.KeycloakConstants.DISABLED_SETTING;
import static com.redhat.che.keycloak.shared.KeycloakConstants.REALM_SETTING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.che.api.core.rest.Service;

import com.google.common.collect.ImmutableMap;
import com.redhat.che.keycloak.shared.KeycloakSettings;

/**
 * Defines Keycloak REST API.
 *
 * @author David Festal
 */
@Singleton
@Path("/keycloak")
public class KeycloakService extends Service {

    @Inject
    public KeycloakService(@Named(DISABLED_SETTING) boolean keycloakDisabled,
                           @Named(AUTH_SERVER_URL_SETTING) String serverURL,
                           @Named(REALM_SETTING) String realm,
                           @Named(CLIENT_ID_SETTING) String clientId) {
        KeycloakSettings.set(ImmutableMap.of(DISABLED_SETTING, Boolean.toString(keycloakDisabled),
                                             AUTH_SERVER_URL_SETTING, serverURL,
                                             CLIENT_ID_SETTING, clientId,
                                             REALM_SETTING, realm));
    }

    @GET
    @Path("/settings")
    @Produces(APPLICATION_JSON)
    public Map<String, String> settings() {
        return KeycloakSettings.get();
    }
}
