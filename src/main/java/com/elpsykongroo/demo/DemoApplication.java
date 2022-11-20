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

package com.elpsykongroo.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.nativex.MyBatisResourcesScan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MyBatisResourcesScan(typeAliasesPackages = "com.elpsykongroo.demo.entity", mapperLocationPatterns = "mapper/**/*Mapper.xml")
@MapperScan(basePackages = "com.elpsykongroo.demo.mapper", sqlSessionTemplateRef = "sqlSessionTemplate")
//@ServletComponentScan
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

//    @Bean
//    public NewTopic topic() {
//        return TopicBuilder.name("demo")
//                .partitions(1)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public ApplicationRunner runner(KafkaTemplate<String, String> template) {
//        return args -> {
//            template.send("demo", "test");
//        };
//    }
//
//    @KafkaListener(id = "0", topics = "demo")
//    public void listen(String in) {
//        System.out.println(in);
//    }

//    @Bean
//    ApplicationRunner runner(DemoMapper mapper) {
//        return args -> {
//            mapper.test();
//        };
//    }
}
