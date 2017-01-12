package com.aware.plugin.fitbit;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.ParameterList;
import com.github.scribejava.core.oauth.OAuth20Service;

import java.util.Map;

/**
 * Created by sklakegg on 02/12/16.
 * Edited by denzilferreira on 11.1.2016
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
}