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

package com.elpsykongroo.demo.utils;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.PemUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

import java.nio.file.Paths;
public final class SSLUtils {
    private SSLUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static SSLContext getSSLContext(String caPath, String certPath, String keyPath) {
        X509ExtendedKeyManager keyManager = 
            PemUtils.loadIdentityMaterial(Paths.get(certPath), Paths.get(keyPath));
        X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(Paths.get(caPath));

        SSLFactory sslFactory =
                SSLFactory.builder()
                          .withIdentityMaterial(keyManager)
                          .withTrustMaterial(trustManager).build();

        return sslFactory.getSslContext() ;
    }
}
