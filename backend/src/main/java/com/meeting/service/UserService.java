package com.meeting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meeting.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 用户注册
     */
    User register(String username, String password, String email);

    /**
     * 验证密码
     */
    boolean verifyPassword(String rawPassword, String encodedPassword);

    /**
     * 加密密码
     */
    String encodePassword(String password);
}
