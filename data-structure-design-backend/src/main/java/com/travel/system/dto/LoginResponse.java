package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应结果。
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /**
     * 是否登录成功。
     */
    private boolean success;

    /**
     * 用户展示名称。
     */
    private String displayName;

    /**
     * 登录令牌（演示环境使用简单令牌，生产环境应使用 JWT/OAuth2）。
     */
    private String token;

    /**
     * 用户提示信息。
     */
    private String message;
}
