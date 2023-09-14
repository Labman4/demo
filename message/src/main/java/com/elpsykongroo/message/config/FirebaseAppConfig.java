/*
 * Copyright 2022-2022 the original author or authors.
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


package com.elpsykongroo.message.config;

import com.elpsykongroo.base.config.ServiceConfig;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
public class FirebaseAppConfig {

    @Autowired
    private ServiceConfig serviceConfig;

    @Bean
    public FirebaseApp firebaseApp () {
        HttpHost proxy = new HttpHost(serviceConfig.getUrl().getProxy(), serviceConfig.getUrl().getProxyPort());
        final ApacheHttpTransport apacheHttpTransport = new ApacheHttpTransport.Builder().setProxy(proxy).build();
        HttpTransportFactory httpTransportFactory = new HttpTransportFactory() {
            @Override
            public HttpTransport create() {
                return apacheHttpTransport;
            }
        };

        GoogleCredentials credentials = null;
        try {
            credentials = GoogleCredentials.fromStream(new FileInputStream(serviceConfig.getCredentialsPath()), httpTransportFactory)
                    .createScoped(Arrays.asList("https://www.googleapis.com/auth/firebase.messaging"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setHttpTransport(apacheHttpTransport)
                .build();
        return FirebaseApp.initializeApp(options);
    }
}
