package com.travel.system.controller;

import com.travel.system.dto.LoginRequest;
import com.travel.system.dto.LoginResponse;
import com.travel.system.dto.RegisterRequest;
import com.travel.system.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器。
 * <p>
 * 当前阶段提供轻量级数据库用户体系，保证前端具备完整“注册-登录-进入系统”的交互闭环。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录接口。
     *
     * @param request 登录请求体（用户名、密码）
     * @return 登录成功后返回登录令牌与用户显示信息
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}
