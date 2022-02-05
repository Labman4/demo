package com.elpsykongroo.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/api")
public class DemoContronller {
    @GetMapping("/api")
    public String access() {
      return "success";
    }
}
