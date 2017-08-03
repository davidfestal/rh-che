package com.redhat.che.keycloak.ide;

import static com.redhat.che.keycloak.shared.KeycloakConstants.AUTH_SERVER_URL_SETTING;
import static com.redhat.che.keycloak.shared.KeycloakConstants.CLIENT_ID_SETTING;
import static com.redhat.che.keycloak.shared.KeycloakConstants.DISABLED_SETTING;
import static com.redhat.che.keycloak.shared.KeycloakConstants.REALM_SETTING;

import java.util.List;
import java.util.Map;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.json.JsonHelper;
import org.eclipse.che.ide.rest.AsyncRequest;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.HTTPHeader;
import org.eclipse.che.ide.util.loging.Log;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.http.client.RequestBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.redhat.che.keycloak.shared.KeycloakConstants;

/**
 * KeycloakAuthAsyncRequestFactory
 */
@Singleton
public class KeycloakAsyncRequestFactory extends AsyncRequestFactory {
    private final DtoFactory dtoFactory;
    private AppContext appContext;
    private boolean keycloakDisabled = false;
    private Promise<Keycloak> keycloak;
    
    @Inject
    public KeycloakAsyncRequestFactory(DtoFactory dtoFactory,
                                       AppContext appContext,
                                       PromiseProvider promiseProvider) {
        super(dtoFactory);
        this.dtoFactory = dtoFactory;
        this.appContext = appContext;
        String keycloakSettings = getKeycloakSettings(KeycloakConstants.getEndpoint(appContext.getMasterEndpoint()));
        Map<String, String> settings =  JsonHelper.toMap(keycloakSettings);
        Log.info(getClass(), "Keycloak settings: ", settings);
        
        this.keycloakDisabled = "true".equals(settings.get(DISABLED_SETTING));
        if (keycloakDisabled) {
            keycloak = null;
        } else {
            keycloak = CallbackPromiseHelper.createFromCallback(new CallbackPromiseHelper.Call<Void, Throwable>() {
                @Override
                public void makeCall(final Callback<Void, Throwable> callback) {
                    ScriptInjector
                    .fromUrl(settings.get(AUTH_SERVER_URL_SETTING) + "/js/keycloak.js")
                    .setCallback(new Callback<Void, Exception>() {
                        @Override
                        public void onSuccess(Void result) {
                            callback.onSuccess(null);
                        }
                        @Override
                        public void onFailure(Exception reason) {
                            callback.onFailure(reason);
                        }
                    })
                    .setWindow(getWindow())
                    .inject();
                }
            }).thenPromise((v) -> Keycloak.init(settings.get(AUTH_SERVER_URL_SETTING),
                                            settings.get(REALM_SETTING),
                                            settings.get(CLIENT_ID_SETTING)));
        }
   }

   @Override
    protected AsyncRequest doCreateRequest(RequestBuilder.Method method, String url, Object dtoBody, boolean async) {
        Preconditions.checkNotNull(method, "Request method should not be a null");
        
        if (keycloakDisabled) {
            return super.doCreateRequest(method, url, dtoBody, async);
        }

        AsyncRequest asyncRequest = new KeycloakAsyncRequest(keycloak, method, url, async);
        if (dtoBody != null) {
            if (dtoBody instanceof List<?>) {
                asyncRequest.data(dtoFactory.toJson((List<?>)dtoBody));
            } else if (dtoBody instanceof String) {
                asyncRequest.data((String)dtoBody);
            } else {
                asyncRequest.data(dtoFactory.toJson(dtoBody));
            }
            asyncRequest.header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON );
        } else if (method.equals(RequestBuilder.POST) || method.equals(RequestBuilder.PUT)) {

            /*
               Here we need to setup wildcard mime type in content-type header, because CORS filter
               responses with 403 error in case if user makes POST/PUT request with null body and without
               content-type header. Setting content-type header with wildcard mime type solves this problem.

               Note, this issue need to be investigated, because the problem may be occurred as a bug in
               CORS filter.
             */

            asyncRequest.header(HTTPHeader.CONTENT_TYPE, MimeType.WILDCARD);
        }
        return asyncRequest;
    }

    public static native String getKeycloakSettings(String keycloakSettingsEndpoint) /*-{
      var myReq = new XMLHttpRequest();
      myReq.open('GET', '' + keycloakSettingsEndpoint, false);
      myReq.send(null);
      return myReq.responseText;
    }-*/;

    public static native JavaScriptObject getWindow() /*-{
      return $wnd;
    }-*/;
}