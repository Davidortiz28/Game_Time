package com.gametime.backend.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("adminUsername", authentication.getName());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
