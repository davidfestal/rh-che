/*******************************************************************************
 * Copyright (c) 2017 Red Hat inc.

 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package com.redhat.che.keycloak.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.che.keycloak.server.oso.service.account.ServiceAccountInfoProvider;
import com.redhat.che.keycloak.shared.KeycloakConstants;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

public class KeycloakAuthenticationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakAuthenticationFilter.class);

    private boolean keycloakDisabled;
    private String authServerUrl;
    private String realm;
    private PublicKey publicKey = null;

    @Inject
    private KeycloakUserChecker userChecker;

    @Inject
    private ServiceAccountInfoProvider serviceAccountInfoProvider;

    @Inject
    public KeycloakAuthenticationFilter(@Named(KeycloakConstants.DISABLED_SETTING) boolean keycloakDisabled,
                                        @Named(KeycloakConstants.AUTH_SERVER_URL_SETTING) String authServerUrl,
                                        @Named(KeycloakConstants.REALM_SETTING) String realm) {
        this.keycloakDisabled = keycloakDisabled;
        this.authServerUrl = authServerUrl;
        this.realm = realm;
        if (keycloakDisabled) {
            LOG.info("Keycloak is disabled");
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (keycloakDisabled) {
            chain.doFilter(req, res);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) req;
        String authHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();
        String requestScheme = req.getScheme();

        if (isSystemStateRequest(requestURI) || isWebsocketRequest(requestURI, requestScheme)
                || isKeycloakSettingsRequest(requestURI) || isWorkspaceAgentRequest(authHeader)
                || isRequestFromGwtFrame(requestURI) || isStackIconRequest(requestURI)) {
            LOG.debug("Skipping {}", requestURI);
            chain.doFilter(req, res);
            return;
        }
        
        if (authHeader == null) {
            LOG.debug("No 'Authorization' header for {}", requestURI);
            send403(res);
            return;
        }

        String tokenContent = authHeader.replace("Bearer ", "");
        Jws<Claims> jwt;
        try {
            
            jwt = Jwts.parser().setSigningKey(getJwtPublicKey(false)).parseClaimsJws(tokenContent);
            LOG.debug("JWT = " + jwt.toString());
            //OK, we can trust this JWT
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            //don't trust the JWT!
            LOG.error("Failed verifying the JWT token", e);
            try {
                LOG.info("Retrying after updating the public key", e);
                jwt = Jwts.parser().setSigningKey(getJwtPublicKey(true)).parseClaimsJws(tokenContent);
                LOG.debug("JWT = " + jwt.toString());
                //OK, we can trust this JWT
            } catch (SignatureException | NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException ee) {
                //don't trust the JWT!
                LOG.error("Failed verifying the JWT token after public key update", e);
                send403(res);
                return;
            }
        }

        if (! userChecker.matchesUsername(authHeader)) {
            LOG.debug("User '{}' doesn't have access to the Che namespace for URI '{}'", jwt.getBody().get("preferred_username"), requestURI);
            send403(res);
            return;
        }
        
        chain.doFilter(req, res);
    }

    private synchronized PublicKey getJwtPublicKey(boolean reset) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if(reset) {
            publicKey = null;
        }
        if (publicKey == null) {
            URL url;
            HttpURLConnection conn;
            try {
                url = new URL(authServerUrl + "/realms/" + realm);
                LOG.info("Pulling realm public key from URL : {}", url);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, String> realmSettings = mapper.readValue(in, Map.class);
                String encodedPublicKey = realmSettings.get("public_key");
                LOG.info("Encoded realm public key = {}", encodedPublicKey);
                byte[] decoded = Base64.getDecoder().decode(encodedPublicKey);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                publicKey = kf.generatePublic(keySpec);
            } catch (IOException e) {
                LOG.error("Exception during retrieval of the Keycloak realm pyublic key", e);
            }
        }
        return publicKey;
    }

    private void send403(ServletResponse res) throws IOException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.sendError(403);
    }
    
    /**
     * @param requestURI
     * @return true if request is made against system state endpoint which is
     *         used in OpenShift liveness & readiness probes, false otherwise
     */
    private boolean isSystemStateRequest(String requestURI) {
        return requestURI.endsWith("/api/system/state");
    }

    /**
     * @param requestURI
     * @return true if request is retrieving a stack icon
     */
    private boolean isStackIconRequest(String requestURI) {
        return requestURI.contains("/api/stack/") && requestURI.endsWith("/icon");
    }
    
    
    /**
     * @param requestURI
     * @return true if request is made against endpoint which provides keycloak
     *         status (enabled / disabled), false otherwise
     */
    private boolean isKeycloakSettingsRequest(String requestURI) {
        return requestURI.endsWith("/keycloak/settings");
    }

    /**
     * @param authHeader
     * @return true if "Authorization" header has valid openshift token that is
     *         used for communication between wsagent and wsmaster, false
     *         otherwise
     */
    private boolean isWorkspaceAgentRequest(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Wsagent")) {
            String token = authHeader.replaceFirst("Wsagent ", "");
            return isValidToken(token);
        }
        return false;
    }

    private boolean isWebsocketRequest(String requestURI, String requestScheme) {
        return requestURI.endsWith("/ws") || requestURI.endsWith("/eventbus") || requestScheme.equals("ws")
                || requestScheme.equals("wss") || requestURI.contains("/websocket/")
                || requestURI.endsWith("/token/user");
    }

    /**
     * OpenShift default service account token is used in "Authorization" header
     * for communication between wsagent and wsmaster. The method checks if the
     * token is valid by fetching namespace info and comparing it with expected 
     * service account namespace
     * 
     * @param token
     * @return true if openshift token is valid and matches service account namespace, false otherwise
     */
    private boolean isValidToken(final String token) {
        LOG.debug("Validating workspace agent token");
        Config config = new ConfigBuilder().withOauthToken(token).build();
        try (OpenShiftClient client = new DefaultOpenShiftClient(config)) {
            String namespace = client.getConfiguration().getNamespace();
            LOG.debug("Validating the token against namespace '{}'", namespace);
            return serviceAccountInfoProvider.getNamespace().equals(namespace);
        } catch (Exception e) {
            LOG.debug("The token is not valid {}", token);
            return false;
        }
    }

    
    /**
     * @param requestURI
     * @return true if request comes from a GWT `Frame`, and thus cannot contain
     * the Keycloak token, false otherwise
     * 
     * There a currently 2 places in the Java Che support that use `Frame` objects
     * to display documentation popups
     * 
     * see issue https://github.com/redhat-developer/rh-che/issues/94
     * 
     */
    private boolean isRequestFromGwtFrame(String requestURI) {
        try {
            // We use a URI here since the expected requests that we want to filter
            // have some parameters. The URI allows matching the URI path precisely
            URI uri = new URI(requestURI);
            return "/api/java/javadoc/find".equals(uri.getPath()) // Java Quick Documentation popup
                || "/api/java/code-assist/compute/info".equals(uri.getPath()); // Java content assist Documentation popup
        } catch (URISyntaxException e) {
        }
        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
