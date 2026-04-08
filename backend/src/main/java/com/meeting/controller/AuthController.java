package com.meeting.controller;

import com.meeting.dto.LoginRequest;
import com.meeting.dto.RegisterRequest;
import com.meeting.dto.Result;
import com.meeting.entity.User;
import com.meeting.service.UserService;
import com.meeting.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword(), request.getEmail());

        String token = jwtUtil.generateToken(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", convertToMap(user));

        return Result.success(data);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        if (!userService.verifyPassword(request.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", convertToMap(user));

        return Result.success(data);
    }

    private Map<String, Object> convertToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("avatar", user.getAvatar());
        return map;
    }
}
