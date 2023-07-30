// /*
//  * Copyright 2022-2022 the original author or authors.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      https://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package com.elpsykongroo.demo.config;
// import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_INSTANCE_ID;


// import io.opentelemetry.api.trace.SpanKind;
// import io.opentelemetry.contrib.samplers.RuleBasedRoutingSampler;
// import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
// import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
// import io.opentelemetry.sdk.resources.Resource;
// import io.opentelemetry.sdk.trace.samplers.Sampler;
// import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
// import org.springframework.context.annotation.Configuration;

// import java.util.UUID;

// @Configuration
// public class OtelCustomizerProviderConfig implements AutoConfigurationCustomizerProvider {
//     @Override
//     public void customize(AutoConfigurationCustomizer autoConfiguration) {
//         // Add additional resource attributes programmatically
//         autoConfiguration.addResourceCustomizer(
//                 (resource, configProperties) ->
//                         resource.merge(
//                                 Resource.builder().put(SERVICE_INSTANCE_ID, UUID.randomUUID().toString()).build()));

//         // Set the sampler to be the default parentbased_always_on, but drop calls to spring
//         // boot actuator endpoints
//         autoConfiguration.addTracerProviderCustomizer(
//                 (sdkTracerProviderBuilder, configProperties) ->
//                         sdkTracerProviderBuilder.setSampler(
//                                 Sampler.parentBased(
//                                         RuleBasedRoutingSampler.builder(SpanKind.SERVER, Sampler.alwaysOn())
//                                                 .drop(SemanticAttributes.HTTP_TARGET, "/actuator.*")
//                                                 .build())));
//     }
// }
