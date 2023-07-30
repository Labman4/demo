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

package com.elpsykongroo.auth.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

public class LiquibaseBeanConfig implements InitializingBean, BeanNameAware, ResourceLoaderAware {

    private SpringLiquibase springLiquibase;

    public LiquibaseBeanConfig(SpringLiquibase springLiquibase) {
        this.springLiquibase = springLiquibase;
    }


    @Override
    public void afterPropertiesSet() throws Exception {

        springLiquibase.afterPropertiesSet();
    }

    @Override
    public void setBeanName(String s) {
        springLiquibase.setBeanName(s);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        springLiquibase.setResourceLoader(resourceLoader);
    }
}
