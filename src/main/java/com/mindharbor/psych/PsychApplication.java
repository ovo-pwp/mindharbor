package com.mindharbor.psych;

import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * sa-token-redis-jackson 自带 Spring 自动配置（META-INF/spring/...AutoConfiguration.imports），
 * 只要依赖在 classpath 就会被无条件注册成 SaTokenDao —— 哪怕根本没配 Redis，
 * 表现就是登录时直接抛 Unable to connect to Redis。
 * 这里排除它的自动配置，改由 SaTokenRedisConfig 按 app.session-store=redis 显式开启。
 */
@SpringBootApplication(exclude = SaTokenDaoRedisJackson.class)
public class PsychApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsychApplication.class, args);
    }
}
