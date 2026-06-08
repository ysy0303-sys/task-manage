package com.example.task.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @GetMapping("/shitu")
    public String shitu() {
        return "shitu";
    }

    @GetMapping("/tasks")
    public String tasks() {
        return "tasks";
    }

    @GetMapping("/messages")
    public String messages() {
        return "messages";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/theme-color")
    public String themeColor() {
        return "theme-color";
    }

    @GetMapping("/riji")
    public String riji() {
        return "riji";
    }
}