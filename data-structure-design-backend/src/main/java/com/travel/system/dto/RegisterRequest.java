package com.travel.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * {@code RegisterRequest} 用于接收用户注册时提交的请求体。
 *
 * <p>当前实现仅包含最基本的字段，后期可根据业务需求扩展为
 * 包含邮箱、手机号、验证码等信息的完整注册表单。
 */
@Data
public class RegisterRequest {

    /** 用户名，不能为空。 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码，不能为空。 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 前端展示的昵称或真实姓名，可选；若未提供可使用 {@code username} 作为默认值。 */
    private String displayName;
}
