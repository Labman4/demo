package com.elpsykongroo.auth.server.utils.jackson;

import com.elpsykongroo.auth.server.service.webauthn.WebAuthnAuthenticationToken;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.List;

public class WebAuthnTokenDeserializer  extends JsonDeserializer<WebAuthnAuthenticationToken> {
        private static final TypeReference<List<GrantedAuthority>> GRANTED_AUTHORITY_LIST = new TypeReference<List<GrantedAuthority>>() {
        };
        private static final TypeReference<Object> OBJECT = new TypeReference<Object>() {
        };

        WebAuthnTokenDeserializer() {
            }

        public WebAuthnAuthenticationToken deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectMapper mapper = (ObjectMapper)jp.getCodec();
            JsonNode jsonNode = (JsonNode)mapper.readTree(jp);
            Boolean authenticated = this.readJsonNode(jsonNode, "authenticated").asBoolean();
            JsonNode principalNode = this.readJsonNode(jsonNode, "principal");
            Object principal = this.getPrincipal(mapper, principalNode);
            JsonNode credentialsNode = this.readJsonNode(jsonNode, "credentials");
            Object credentials = this.getCredentials(credentialsNode);
            List<GrantedAuthority> authorities = (List)mapper.readValue(this.readJsonNode(jsonNode, "authorities").traverse(mapper), GRANTED_AUTHORITY_LIST);
            WebAuthnAuthenticationToken token = !authenticated ? WebAuthnAuthenticationToken.unauthenticated(principal, credentials) : WebAuthnAuthenticationToken.authenticated(principal, credentials, authorities);
            JsonNode detailsNode = this.readJsonNode(jsonNode, "details");
            if (!detailsNode.isNull() && !detailsNode.isMissingNode()) {
                Object details = mapper.readValue(detailsNode.toString(), OBJECT);
                token.setDetails(details);
            } else {
                token.setDetails((Object)null);
            }

            return token;
        }

        private Object getCredentials(JsonNode credentialsNode) {
            return !credentialsNode.isNull() && !credentialsNode.isMissingNode() ? credentialsNode.asText() : null;
        }

        private Object getPrincipal(ObjectMapper mapper, JsonNode principalNode) throws IOException, JsonParseException, JsonMappingException {
            return principalNode.isObject() ? mapper.readValue(principalNode.traverse(mapper), Object.class) : principalNode.asText();
        }

        private JsonNode readJsonNode(JsonNode jsonNode, String field) {
            return (JsonNode)(jsonNode.has(field) ? jsonNode.get(field) : MissingNode.getInstance());
        }


}
