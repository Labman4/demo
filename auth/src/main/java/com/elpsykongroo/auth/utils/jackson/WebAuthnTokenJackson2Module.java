/*
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elpsykongroo.auth.utils.jackson;

import com.elpsykongroo.auth.entity.user.Authority;
import com.elpsykongroo.auth.security.provider.WebAuthnAuthenticationToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.security.jackson2.SecurityJackson2Modules;


public class WebAuthnTokenJackson2Module extends SimpleModule {
    public WebAuthnTokenJackson2Module() {
        super(WebAuthnTokenJackson2Module.class.getName(), new Version(1, 0, 0, (String)null, (String)null, (String)null));
    }

    public void setupModule(Module.SetupContext context) {
        SecurityJackson2Modules.enableDefaultTyping((ObjectMapper)context.getOwner());
        context.setMixInAnnotations(WebAuthnAuthenticationToken.class, WebAuthnTokenMixin.class);
        context.setMixInAnnotations(Authority.class, AuthorityMixin.class);
    }
}