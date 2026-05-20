package com.tihu.backend.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token权限配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token拦截器，定义需要登录的路由
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 定义路由权限
            SaRouter.match("/api/**")                    // 匹配 /api/ 下的所有路由
                    .notMatch("/api/users/register")     // 放行注册接口
                    .notMatch("/api/users/login")        // 放行登录接口
                    .notMatch("/api/books/**")          // 放行所有书籍查询接口
                    .check(r -> StpUtil.checkLogin());   // 其他路由需要登录

            // 管理员路由
            SaRouter.match("/api/*/admin/**")
                    .check(r -> StpUtil.checkRole("ADMIN"));
        })).addPathPatterns("/**");
    }
}

