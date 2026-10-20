package com.mindharbor.psych.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 不写死 DbType，由 MyBatis-Plus 从连接 URL 自动识别（H2 / MySQL 都能用）
        PaginationInnerInterceptor page = new PaginationInnerInterceptor();
        page.setMaxLimit(200L);
        interceptor.addInnerInterceptor(page);
        return interceptor;
    }
}
