package com.tihu.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/yjm")
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

//    @GetMapping
//    public String hello1(){
//        return "GET喵喵";
//    }

    @RequestMapping("/hello")
    public String hello2() {
        return "hello world";
    }

    @GetMapping("/{id}")
    public String y(@PathVariable int id) {
        System.out.printf("ID=%d\n", id);
        return "参数请求成功";
    }

    @GetMapping
    public String x(@RequestParam int id, @RequestParam String name) {
        System.out.printf("ID=%d,name=%s\n", id, name);
        return "请求成功";
    }

    @PostMapping
    public String z(@RequestBody Map<String, String> map) {
        System.out.println(map.toString());
        return "接口请求成功";
    }
}
