package com.example.task.service.impl;

import com.example.task.entity.User;
import com.example.task.repository.UserRepository;
import com.example.task.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(User user) {
        log.info("开始执行用户注册，用户名：{}", user.getUsername());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            log.error("注册失败：用户名已存在 -> {}", user.getUsername());
            throw new RuntimeException("用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        log.info("注册成功，用户ID：{}", savedUser.getId());
        return savedUser;
    }

    @Override
    public User login(String username, String password) {
        log.info("用户登录请求，用户名：{}", username);

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            log.error("登录失败：用户名不存在 -> {}", username);
            return null;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.error("登录失败：密码错误，用户名：{}", username);
            return null;
        }

        log.info("登录成功，用户名：{}", username);
        return user;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) return false;
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Override
    public User updateProfile(Long userId, String nickname) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        user.setNickname(nickname);
        return userRepository.save(user);
    }

    @Override
    public User updateAvatar(Long userId, String avatarPath) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        user.setAvatar(avatarPath);
        return userRepository.save(user);
    }
}