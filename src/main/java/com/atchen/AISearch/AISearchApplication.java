package com.atchen.AISearch;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.atchen.AISearch.mapper")
public class AISearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(AISearchApplication.class, args);
    }
}
