package com.travel.system.service;

import com.travel.system.dto.LoginRequest;
import com.travel.system.dto.LoginResponse;
import com.travel.system.dto.RegisterRequest;
import com.travel.system.model.UserAccount;
import com.travel.system.mapper.UserAccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * {@code AuthService} 提供用户认证与注册的业务实现。
 *
 * <p>核心职责包括：
 *
 * <ul>
 *   <li>验证登录凭证（用户名 + 密码），比对密码哈希；</li>
 *   <li>为通过验证的用户生成登录令牌（演示用的伪 token）；</li>
 *   <li>处理用户注册，保存新用户的密码哈希、显示名称及兴趣标签；</li>
 *   <li>在系统初始化阶段创建若干示例账号（seed users）。</li>
 * </ul>
 *
 * <p>所有对外暴露的方法均返回 {@link LoginResponse}，统一封装登录成功信息与 token。
 *
 * @author 自动生成
 */
@Service
public class AuthService {

    /** 用户账户 MyBatis Mapper，用于查询、保存用户信息。 */
    private final UserAccountMapper userAccountMapper;

    public AuthService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    /**
     * 登录流程：
     *
     * <ol>
     *   <li>根据用户名查询用户记录；</li>
     *   <li>对前端提交的明文密码进行 SHA‑256 哈希，与数据库保存的 {@code passwordHash} 对比；</li>
     *   <li>若验证成功，生成一个演示用的 token 并返回 {@link LoginResponse}；</li>
     *   <li>若用户名不存在或密码不匹配，抛出 {@link ResponseStatusException}（401 Unauthorized）。</li>
     * </ol>
     *
     * @param request 包含用户名与明文密码的登录请求体
     * @return 登录成功后的响应对象
     */
    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountMapper.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未注册或用户名错误"));


        // 对比密码哈希，确保安全性
        if (!sha256(request.getPassword()).equals(user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，请重试");
        }

        // 演示用 token，可在实际项目中替换为 JWT 等实现
        String token = "demo-token-" + UUID.randomUUID();
        return new LoginResponse(true, user.getDisplayName(), token, "登录成功，欢迎进入个性化旅游系统");
    }

    /**
     * 注册流程：
     *
     * <ol>
     *   <li>检查用户名是否已被占用，若已存在则抛出 409 Conflict；</li>
     *   <li>将明文密码使用 SHA‑256 加密后存入 {@code passwordHash}；</li>
     *   <li>保存新用户实体并自动分配 ID；</li>
     *   <li>生成并返回登录 token，实现注册后即登录的体验。</li>
     * </ol>
     *
     * @param request 包含注册所需字段的请求体
     * @return 注册成功后的登录响应
     */
    public LoginResponse register(RegisterRequest request) {
        if (userAccountMapper.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在，请更换后重试");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setPasswordHash(sha256(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        // 示例兴趣标签，可根据业务需求动态填充
        user.setInterests("校园,博物馆,美食");
        userAccountMapper.save(user);

        String token = "demo-token-" + UUID.randomUUID();
        return new LoginResponse(true, user.getDisplayName(), token, "注册成功，已自动登录");
    }

    /**
     * 系统初始化阶段确保有若干演示账号存在。
     *
     * <p>如果数据库中已有 10 条以上用户记录，则认为已经完成初始化，直接返回。
     * 否则遍历预设的 {@link SeedUser} 列表，逐条检查用户名是否已存在，未存在则创建。
     */
    public void ensureSeedUsers() {
        if (userAccountMapper.count() >= 10) {
            return;
        }

        List<SeedUser> seedUsers = List.of(
                new SeedUser("demo", "123456", "演示用户", "校园,博物馆,小吃"),
                new SeedUser("admin", "admin123", "系统管理员", "景区,路线规划"),
                new SeedUser("guest", "guest123", "游客用户", "校园,美食"),
                new SeedUser("alice", "alice123", "Alice", "博物馆,咖啡馆"),
                new SeedUser("bob", "bob123", "Bob", "校园,食堂"),
                new SeedUser("cathy", "cathy123", "Cathy", "景区,日记"),
                new SeedUser("david", "david123", "David", "路线,骑行"),
                new SeedUser("eva", "eva123", "Eva", "摄影,校园"),
                new SeedUser("frank", "frank123", "Frank", "美食,咖啡"),
                new SeedUser("grace", "grace123", "Grace", "景区,博物馆")
        );

        for (SeedUser seedUser : seedUsers) {
            userAccountMapper.findByUsername(seedUser.username()).orElseGet(() -> {
                UserAccount user = new UserAccount();
                user.setUsername(seedUser.username());
                user.setPasswordHash(sha256(seedUser.password()));
                user.setDisplayName(seedUser.displayName());
                user.setInterests(seedUser.interests());
                return userAccountMapper.save(user);
            });
        }
    }

    /**
     * 对输入字符串执行 SHA‑256 哈希并返回十六进制表示。
     *
     * @param source 明文字符串
     * @return SHA‑256 哈希的十六进制字符串
     */
    private String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            // SHA‑256 在任何标准 JDK 中均应可用，若不可用则属于系统错误
            throw new IllegalStateException("系统缺少 SHA-256 算法实现", exception);
        }
    }

    /**
     * 用于 seed 用户信息的内部记录类型。
     *
     * @param username   用户名
     * @param password   明文密码（将被哈希后存储）
     * @param displayName 显示名称
     * @param interests   兴趣标签，逗号分隔
     */
    private record SeedUser(String username, String password, String displayName, String interests) {
    }
}
