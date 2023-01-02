package com.elpsykongroo.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "service")
@Data
public class ServiceConfig {

    private  ES es ;

    //public ca
    private SSL ssl;

    @Data
    public static class SSL {
        
        private String type;

        private String ca ;

        private String cert;

        private String key;
    }

    @Data
    public static class ES {

        private TimeOut timeout;

        private String user;

        private String pass;

        //self ca
        private SSL ssl ;

        private String[] nodes;

    }

    @Data
    public static class TimeOut {

        private String connect ;

        private String socket;

    }
}
