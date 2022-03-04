package com.elpsykongroo.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class DemoContronller {
    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("")
    public String access() {
      return "success";
    }
}
