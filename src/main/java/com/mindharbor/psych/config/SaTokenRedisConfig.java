package com.mindharbor.psych.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 会话存储切换：只有显式配置 app.session-store=redis 才启用。
 * 不配就用 Sa-Token 默认的内存存储（本地单机开发）。
 *
 * 用自定义开关而不是探测 spring.data.redis.host，是因为后者受 RedisProperties
 * 默认值影响，容易在没装 Redis 的环境里被误触发，导致登录直接 500。
 *
 * 注意：SaTokenDaoRedisJackson 必须显式调 init(connectionFactory)，
 * 它内部靠这个方法构建 StringRedisTemplate / ObjectRedisTemplate 并置 isInit=true，
 * 直接 new 出来用会抛「尚未初始化」。
 */
@Configuration
@ConditionalOnProperty(prefix = "app", name = "session-store", havingValue = "redis")
public class SaTokenRedisConfig {

    @Bean
    public SaTokenDao saTokenDao(RedisConnectionFactory connectionFactory) {
        SaTokenDaoRedisJackson dao = new SaTokenDaoRedisJackson();
        dao.init(connectionFactory);
        return dao;
    }
}
