package com.travel.system.service;

import com.travel.system.dto.LoginRequest;
import com.travel.system.dto.LoginResponse;
import com.travel.system.dto.RegisterRequest;
import com.travel.system.model.UserAccount;
import com.travel.system.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;

    public AuthService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，请重试"));

        if (!sha256(request.getPassword()).equals(user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，请重试");
        }

        String token = "demo-token-" + UUID.randomUUID();
        return new LoginResponse(true, user.getDisplayName(), token, "登录成功，欢迎进入个性化旅游系统");
    }

    public LoginResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在，请更换后重试");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setPasswordHash(sha256(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setInterests("校园,博物馆,美食");
        userAccountRepository.save(user);

        String token = "demo-token-" + UUID.randomUUID();
        return new LoginResponse(true, user.getDisplayName(), token, "注册成功，已自动登录");
    }

    public void ensureSeedUsers() {
        if (userAccountRepository.count() >= 10) {
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
            userAccountRepository.findByUsername(seedUser.username()).orElseGet(() -> {
                UserAccount user = new UserAccount();
                user.setUsername(seedUser.username());
                user.setPasswordHash(sha256(seedUser.password()));
                user.setDisplayName(seedUser.displayName());
                user.setInterests(seedUser.interests());
                return userAccountRepository.save(user);
            });
        }
    }

    private String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("系统缺少 SHA-256 算法实现", exception);
        }
    }

    private record SeedUser(String username, String password, String displayName, String interests) {
    }
}
