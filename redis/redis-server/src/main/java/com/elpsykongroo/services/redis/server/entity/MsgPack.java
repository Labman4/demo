package com.elpsykongroo.services.redis.server.entity;

import lombok.Data;

import java.time.Instant;

@Data
public class MsgPack{
    private Instant ca;
    private Instant eo;
    private String at;
    private String it;
    private String rt;
    private byte[] n;
    private String e;
    private String u;
    private String[] g;
    private String pu;


}