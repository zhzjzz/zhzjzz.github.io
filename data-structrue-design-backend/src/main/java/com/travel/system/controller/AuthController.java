package com.travel.system.controller;

import com.travel.system.dto.LoginRequest;
import com.travel.system.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 用户认证控制器。
 * <p>
 * 当前阶段用于课程演示：提供基础登录能力，保证前端具备完整“登录-进入系统”的交互闭环。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 演示账号表：key=用户名，value[0]=密码，value[1]=显示名。
     */
    private static final Map<String, String[]> DEMO_USERS = Map.of(
            "demo", new String[]{"123456", "演示用户"},
            "admin", new String[]{"admin123", "系统管理员"},
            "guest", new String[]{"guest123", "游客用户"}
    );

    /**
     * 登录接口。
     *
     * @param request 登录请求体（用户名、密码）
     * @return 登录成功后返回登录令牌与用户显示信息
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String[] userInfo = DEMO_USERS.get(request.getUsername());
        if (userInfo == null || !userInfo[0].equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，请重试");
        }

        // 演示令牌：课程环境仅用于前端状态保存；真实项目请改造为安全签名令牌。
        String token = "demo-token-" + request.getUsername();
        return new LoginResponse(true, userInfo[1], token, "登录成功，欢迎进入个性化旅游系统");
    }
}
