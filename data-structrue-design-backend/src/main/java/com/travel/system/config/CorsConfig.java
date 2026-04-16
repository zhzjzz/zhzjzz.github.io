package com.travel.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域资源共享 (CORS) 配置类
 * <p>
 * 配置 Spring Boot 应用的跨域访问策略，允许前端应用从不同域名访问后端 API。
 * 在开发环境中通常配置为允许所有来源，生产环境应限制为特定的前端域名
 * </p>
 * <p>
 * 当前配置：
 * <ul>
 *     <li>允许所有路径 (/**)</li>
 *     <li>允许所有来源 (*)</li>
 *     <li>允许的 HTTP 方法：GET, POST, PUT, DELETE, PATCH</li>
 *     <li>允许所有请求头</li>
 * </ul>
 * </p>
 * <p>
 * <strong>注意：</strong>生产环境应修改 allowedOriginPatterns 为具体域名，
 * 避免安全风险
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see WebMvcConfigurer Spring MVC 配置接口
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 添加 CORS 跨域映射配置
     * <p>
     * 配置允许跨域访问的详细规则，包括允许的源、方法、请求头等
     * </p>
     *
     * @param registry CORS 注册器，用于添加跨域映射规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 配置所有路径允许跨域访问
        registry.addMapping("/**")
                // 允许所有来源（生产环境应限制为特定域名）
                .allowedOriginPatterns("*")
                // 允许的 HTTP 请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                // 允许所有请求头
                .allowedHeaders("*");
    }
}
