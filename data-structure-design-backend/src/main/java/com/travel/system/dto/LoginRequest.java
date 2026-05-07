package com.travel.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数。
 * <p>
 * 课程设计阶段先采用轻量级账号密码校验，后续可平滑替换为数据库用户体系。
 */
@Data
public class LoginRequest {

    /**
     * 用户名（不能为空）。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码（不能为空）。
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
