/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.social.vkontakte;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorResponseException;
import java.net.URLEncoder;
import java.net.URLDecoder;


import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class VkontakteIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://oauth.vk.com/authorize";
	public static final String TOKEN_URL = "https://oauth.vk.com/access_token";
	public static final String PROFILE_URL = "https://api.vk.com/method/users.get?fields=id,screen_name,first_name,last_name&access_token=%s&v=5.78";
	public static final String DEFAULT_SCOPE = "email";
	public static final String OAUTH2_VK_PARAMETER_EMAIL = "email";

	public VkontakteIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	@Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        String accessToken = extractTokenFromResponse(response, getAccessTokenResponseParameter());
        String email = extractTokenFromResponse(response, OAUTH2_VK_PARAMETER_EMAIL); // method extracts any param, not only token

        BrokeredIdentityContext context = null;
        
        if (accessToken == null) {
            throw new IdentityBrokerException("No access token available in OAuth server response: " + response);
        }
        
		try {
			JsonNode raw_profile = SimpleHttp.doGet(String.format(PROFILE_URL, accessToken), session).header("content-type", "application/json; charset=utf-8;").asJson();
			JsonNode profile = raw_profile.get("response").get(0);
			((ObjectNode)profile).put("email", email);
			
			context = extractIdentityFromProfile(null, profile);
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from vkontakte.", e);
		}        
        
		
        context.getContextData().put(FEDERATED_ACCESS_TOKEN, accessToken);
        return context;
    }	

	@Override
	protected boolean supportsExternalExchange() {
		return true;
	}

	@Override
	protected String getProfileEndpointForValidation(EventBuilder event) {
		return PROFILE_URL;
	}

	@Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
		String id = getJsonProperty(profile, "id");

		BrokeredIdentityContext user = new BrokeredIdentityContext(id);

		String email = getJsonProperty(profile, "email");

		user.setEmail(email);

		String username = getJsonProperty(profile, "screen_name");

		if (username == null) {
            if (email != null) {
                username = email;
            } else {
                username = id;
            }
        }

		user.setUsername(username);

		String firstName = getJsonProperty(profile, "first_name");
		String lastName = getJsonProperty(profile, "last_name");

		// apply UTF-8 encoding
		try {
			firstName = new String(firstName.getBytes(), "UTF-8"); 
			lastName = new String(lastName.getBytes(), "UTF-8"); 
		} catch (Exception e) {
			logger.error("Failed to decode first_name and last_name as UTF-8", e);
		}
		

		if (lastName == null) {
            lastName = "";
        } else {
            lastName = " " + lastName;
        }

		user.setName(firstName + lastName);
		user.setIdpConfig(getConfig());
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
