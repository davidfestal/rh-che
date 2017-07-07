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
package org.eclipse.che.wsagent.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.redhat.che.keycloak.server.KeycloakHttpJsonRequestFactory;

import org.eclipse.che.api.core.rest.ApiInfoService;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.inject.DynaModule;

import com.google.inject.name.Names;
import java.net.URI;
import com.redhat.che.keycloak.server.KeycloakPropertiesProvider;
import org.eclipse.che.UriApiEndpointProvider;

import javax.inject.Named;

/**
 * Mandatory modules of workspace agent
 *
 * @author Evgen Vidolob
 * @author Sergii Kabashniuk
 */
@DynaModule
public class WsAgentModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ApiInfoService.class);
        install(new org.eclipse.che.security.oauth.OAuthAgentModule());
        install(new org.eclipse.che.api.core.rest.CoreRestModule());
        install(new org.eclipse.che.api.core.util.FileCleaner.FileCleanerModule());
        install(new org.eclipse.che.api.project.server.ProjectApiModule());
        install(new org.eclipse.che.commons.schedule.executor.ScheduleModule());
        install(new org.eclipse.che.plugin.ssh.key.SshModule());
        install(new org.eclipse.che.api.languageserver.LanguageServerModule());
        install(new org.eclipse.che.api.debugger.server.DebuggerModule());
        install(new org.eclipse.che.api.git.GitModule());
        install(new org.eclipse.che.git.impl.jgit.JGitModule());
        install(new org.eclipse.che.api.core.jsonrpc.impl.JsonRpcModule());
        install(new org.eclipse.che.api.core.websocket.impl.WebSocketModule());

        bind(Boolean.class).annotatedWith(Names.named("che.keycloak.disabled")).toProvider(KeycloakPropertiesProvider.class);
        bind(HttpJsonRequestFactory.class).to(KeycloakHttpJsonRequestFactory.class);
    }

    //it's need for WSocketEventBusClient and in the future will be replaced with the property
    @Named("notification.client.event_subscriptions")
    @Provides
    @SuppressWarnings("unchecked")
    Pair<String, String>[] eventSubscriptionsProvider(@Named("event.bus.url") String eventBusURL) {
        return new Pair[]{Pair.of(eventBusURL, "")};
    }

    //it's need for EventOriginClientPropagationPolicy and in the future will be replaced with the property
    @Named("notification.client.propagate_events")
    @Provides
    @SuppressWarnings("unchecked")
    Pair<String, String>[] propagateEventsProvider(@Named("event.bus.url") String eventBusURL) {
        return new Pair[]{Pair.of(eventBusURL, "")};
    }
}