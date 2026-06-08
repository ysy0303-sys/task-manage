package com.example.task.service;

import com.example.task.entity.User;

public interface UserService {

    User register(User user);

    User login(String username, String password);

    boolean changePassword(Long userId, String oldPassword, String newPassword);

    User updateProfile(Long userId, String nickname);

    User updateAvatar(Long userId, String avatarPath);
}