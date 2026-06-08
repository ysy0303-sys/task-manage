package com.example.task.controller;

import com.example.task.entity.User;
import com.example.task.entity.UserSettings;
import com.example.task.repository.UserSettingsRepository;
import com.example.task.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping({"/user/index", "/user/task"})
    public String indexPage(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return "redirect:/login";
        }
        return "redirect:/index";
    }

    @PostMapping("/register")
    @ResponseBody
    public Object register(@RequestParam String username,
                           @RequestParam String password) {
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);

            userService.register(user);

            return Map.of(
                    "success", true,
                    "msg", "注册成功"
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "msg", e.getMessage()
            );
        }
    }

    @PostMapping("/login")
    @ResponseBody
    public Object login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session) {

        User user = userService.login(username, password);

        if (user != null) {
            session.setAttribute("loginUser", user);
            return Map.of(
                    "success", true,
                    "msg", "登录成功"
            );
        } else {
            return Map.of(
                    "success", false,
                    "msg", "用户名或密码错误"
            );
        }
    }

    @PostMapping("/user/change-password")
    @ResponseBody
    public Object changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Map.of("success", false, "msg", "请先登录");
        }
        boolean ok = userService.changePassword(user.getId(), oldPassword, newPassword);
        if (ok) {
            return Map.of("success", true, "msg", "密码修改成功");
        }
        return Map.of("success", false, "msg", "原密码错误");
    }

    @GetMapping("/user/me")
    @ResponseBody
    public Object currentUser(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Map.of("success", false, "msg", "请先登录");
        }
        return Map.of(
                "success", true,
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        );
    }

    @PostMapping("/user/update-profile")
    @ResponseBody
    public Object updateProfile(@RequestParam String nickname, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Map.of("success", false, "msg", "请先登录");
        }
        User updated = userService.updateProfile(user.getId(), nickname);
        session.setAttribute("loginUser", updated);
        return Map.of("success", true, "msg", "昵称修改成功");
    }

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private File getAvatarDir() {
        File dir = new File(uploadDir, "avatars");
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), dir.getPath());
        }
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    @PostMapping("/user/upload-avatar")
    @ResponseBody
    public Object uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Map.of("success", false, "msg", "请先登录");
        }
        if (file.isEmpty()) {
            return Map.of("success", false, "msg", "请选择文件");
        }
        try {
            File dir = getAvatarDir();
            String ext = file.getOriginalFilename() != null &&
                    file.getOriginalFilename().contains(".") ?
                    file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")) : ".jpg";
            String filename = "avatar_" + user.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            File dest = new File(dir, filename);
            file.transferTo(dest);
            String avatarPath = "/uploads/avatars/" + filename;
            User updated = userService.updateAvatar(user.getId(), avatarPath);
            session.setAttribute("loginUser", updated);
            return Map.of("success", true, "msg", "头像上传成功", "avatar", avatarPath);
        } catch (IOException e) {
            return Map.of("success", false, "msg", "上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/uploads/avatars/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        File file = new File(getAvatarDir(), filename);
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType;
        String f = filename.toLowerCase();
        if (f.endsWith(".png")) mediaType = MediaType.IMAGE_PNG;
        else if (f.endsWith(".gif")) mediaType = MediaType.IMAGE_GIF;
        else if (f.endsWith(".webp")) mediaType = MediaType.valueOf("image/webp");
        else mediaType = MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    // ========== 用户设置 ==========

    @GetMapping("/user/settings")
    @ResponseBody
    public Object getUserSettings(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Map.of("success", false, "msg", "请先登录");
        }
        Optional<UserSettings> opt = userSettingsRepository.findByUserId(user.getId());
        UserSettings settings = opt.orElseGet(() -> new UserSettings(user.getId()));
        return Map.of(
                "success", true,
                "weeklyReportDay", settings.getWeeklyReportDay() != null ? settings.getWeeklyReportDay() : 0
        );
    }

    @PostMapping("/user/settings/weekly-report")
    @ResponseBody
    public Object setWeeklyReportDay(@RequestParam Integer day, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Map.of("success", false, "msg", "请先登录");
        }
        if (day < 0 || day > 7) {
            return Map.of("success", false, "msg", "参数错误：day 应为 0-7（0=关闭，1-7=周一到周日）");
        }

        UserSettings settings = userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSettings s = new UserSettings(user.getId());
                    return s;
                });

        if (day == 0) {
            settings.setWeeklyReportDay(null);
        } else {
            settings.setWeeklyReportDay(day);
        }
        userSettingsRepository.save(settings);

        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String msg = day == 0 ? "周报已关闭" : ("周报发送日已设为 " + dayNames[day]);
        return Map.of("success", true, "msg", msg);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
