package com.travel.system.controller;

import com.travel.system.dto.LoginRequest;
import com.travel.system.dto.LoginResponse;
import com.travel.system.dto.RegisterRequest;
import com.travel.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * {@code AuthController} 负责处理用户认证相关的 HTTP 请求。
 *
 * <p>提供两套核心接口：
 *
 * <ul>
 *   <li>登录（{@code /api/auth/login}）——根据用户名、密码返回令牌及用户信息；</li>
 *   <li>注册（{@code /api/auth/register}）——创建新用户并返回同登录响应。</li>
 * </ul>
 *
 * <p>控制器本身不涉及业务实现，所有逻辑全部委托给 {@link AuthService}。
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "用户认证", description = "用户登录、注册等相关接口")
public class AuthController {

    /** 业务层服务，负责用户的登录、注册以及令牌生成。 */
    private final AuthService authService;

    /**
     * 构造函数通过依赖注入获取 {@link AuthService} 实例。
     *
     * @param authService 业务层认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录接口。
     *
     * @param request 包含 {@code username} 与 {@code password} 的登录请求体
     * @return 登录成功后返回的 {@link LoginResponse}
     */
    @Operation(summary = "用户登录", description = "根据用户名和密码进行身份验证，返回访问令牌")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // 委托 AuthService 完成认证并返回结果
        return authService.login(request);
    }

    /**
     * 注册接口。
     *
     * @param request 包含注册所需字段的请求体
     * @return 注册成功后返回的 {@link LoginResponse}
     */
    @Operation(summary = "用户注册", description = "创建新用户账号，注册成功后自动登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "用户名已存在或参数不合法")
    })
    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        // 直接走业务层的注册流程，返回与登录相同的响应结构
        return authService.register(request);
    }
}
