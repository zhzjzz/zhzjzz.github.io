package com.travel.system.controller;

import com.travel.system.dto.LoginRequest;
import com.travel.system.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 用户认证控制器。
 * <p>
 * 当前阶段用于课程演示：提供基础登录能力，保证前端具备完整“登录-进入系统”的交互闭环。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 演示账号表：key=用户名，value[0]=密码哈希，value[1]=显示名。
     */
    private static final Map<String, String[]> DEMO_USERS = Map.of(
            "demo", new String[]{"8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92", "演示用户"},
            "admin", new String[]{"240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9", "系统管理员"},
            "guest", new String[]{"6b93ccba414ac1d0ae1e77f3fac560c748a6701ed6946735a49d463351518e16", "游客用户"}
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
        if (userInfo == null || !userInfo[0].equals(sha256(request.getPassword()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，请重试");
        }

        // 使用随机 UUID 生成演示令牌，避免可预测令牌带来的伪造风险。
        String token = "demo-token-" + UUID.randomUUID();
        return new LoginResponse(true, userInfo[1], token, "登录成功，欢迎进入个性化旅游系统");
    }

    /**
     * 对密码进行 SHA-256 哈希，避免明文密码存储。
     */
    private String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("系统缺少 SHA-256 算法实现", exception);
        }
    }
}
