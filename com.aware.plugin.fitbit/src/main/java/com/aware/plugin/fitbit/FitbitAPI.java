package com.aware.plugin.fitbit;

import android.content.Context;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;

import com.aware.Aware;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.ParameterList;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

/**
 * Created by sklakegg on 02/12/16.
 */

public class FitbitAPI extends DefaultApi20 {

    private FitbitAPI() {
    }

    private static class InstanceHolder {
        private static final FitbitAPI INSTANCE = new FitbitAPI();
    }

    static FitbitAPI instance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return "https://api.fitbit.com/oauth2/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://www.fitbit.com/oauth2/authorize";
    }

    @Override
    public OAuth20Service createService(OAuthConfig config) {
        return new FitbitOAuthServiceImpl(this, config);
    }

    // Add custom parameters to authorization URL.
    @Override
    public String getAuthorizationUrl(OAuthConfig config, Map<String, String> additionalParams) {
        final ParameterList parameters = new ParameterList(additionalParams);
        parameters.add(OAuthConstants.RESPONSE_TYPE, config.getResponseType());
        parameters.add(OAuthConstants.CLIENT_ID, config.getApiKey());
        parameters.add("expires_in", "31536000");

        final String callback = config.getCallback();
        if (callback != null) {
            parameters.add(OAuthConstants.REDIRECT_URI, callback);
        }

        final String scope = config.getScope();
        if (scope != null) {
            parameters.add(OAuthConstants.SCOPE, scope);
        }

        final String state = config.getState();
        if (state != null) {
            parameters.add(OAuthConstants.STATE, state);
        }

        return parameters.appendTo(getAuthorizationBaseUrl());
    }

    /**
     * Get data from a resource URL endpoint
     * @param context
     * @param resource_url
     * @return
     */
    public static String fetchData(Context context, String resource_url) {
        if (Plugin.fitbitAPI == null || Plugin.fitbitOAUTHToken == null) {
            try {
                restoreFitbitAPI(context);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        OAuthRequest request = new OAuthRequest(Verb.GET, resource_url, Plugin.fitbitAPI);
        request.addHeader("Authorization", " " + Aware.getSetting(context, Settings.OAUTH_TOKEN_TYPE) + " " + Aware.getSetting(context, Settings.OAUTH_TOKEN));

        String metric = "";
        if (Aware.getSetting(context, Settings.UNITS_PLUGIN_FITBIT).equalsIgnoreCase("metric")) metric = "Metric";
        if (Aware.getSetting(context, Settings.UNITS_PLUGIN_FITBIT).equalsIgnoreCase("imperial")) metric = "en_US";
        request.addHeader("Accept-Language", metric);

        Plugin.fitbitAPI.signRequest(Plugin.fitbitOAUTHToken, request);
        Response response = request.send();

        if (response.isSuccessful()) {
            try {
                return response.getBody();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Restore connection to the Fitbit API endpoints
     * @param context
     * @throws JSONException
     */
    public static void restoreFitbitAPI(Context context) throws JSONException {
        if (Aware.getSetting(context, Settings.OAUTH_TOKEN).length() == 0) return;

        Plugin.fitbitOAUTHToken = new OAuth2AccessToken(Aware.getSetting(context, Settings.OAUTH_TOKEN),
                Aware.getSetting(context, Settings.OAUTH_TOKEN_TYPE),
                Integer.valueOf(Aware.getSetting(context, Settings.OAUTH_VALIDITY)),
                "null",
                Aware.getSetting(context, Settings.OAUTH_SCOPES),
                "null");

        JSONObject scopes = new JSONObject(Aware.getSetting(context, Settings.OAUTH_SCOPES));

        String scopes_str = (scopes.getBoolean("activity"))?"activity":"";
        scopes_str += (scopes.getBoolean("heartrate"))?" heartrate":"";
        scopes_str += (scopes.getBoolean("sleep"))?" sleep":"";
        scopes_str += (scopes.getBoolean("settings"))?" settings":"";

        Plugin.fitbitAPI = new ServiceBuilder()
                .apiKey(Aware.getSetting(context, Settings.API_KEY_PLUGIN_FITBIT))
                .scope(scopes_str)
                .responseType("token")
                .callback("fitbit://logincallback")
                .apiSecret(Aware.getSetting(context, Settings.API_SECRET_PLUGIN_FITBIT))
                .build(FitbitAPI.instance());
    }

    /**
     * Initiate authorization flow with Fitbit API on the browser
     *
     * @param context
     * @throws JSONException
     */
    public static void authorizeFitbit(Context context) throws JSONException {
        String scopes = "activity heartrate sleep settings";

        Plugin.fitbitAPI = new ServiceBuilder()
                .apiKey(Aware.getSetting(context, Settings.API_KEY_PLUGIN_FITBIT))
                .scope(scopes)
                .responseType("token")
                .callback("fitbit://logincallback")
                .apiSecret(Aware.getSetting(context, Settings.API_SECRET_PLUGIN_FITBIT))
                .build(FitbitAPI.instance());

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(context, Uri.parse(Plugin.fitbitAPI.getAuthorizationUrl()));
    }
}