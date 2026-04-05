package com.paypocket.controller;

import com.paypocket.exception.DuplicateUserException;
import com.paypocket.exception.UserNotFoundException;
import com.paypocket.model.User;
import com.paypocket.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ========================
    // РЕГИСТРАЦИЯ
    // ========================

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userService.register(username, email, password);
            session.setAttribute("currentUser", user);
            return "redirect:/dashboard";
        } catch (DuplicateUserException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    // ========================
    // ВХОД
    // ========================

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            User user = userService.authenticate(username, password);
            session.setAttribute("currentUser", user);
            return "redirect:/dashboard";
        } catch (UserNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Неверный пароль");
            return "redirect:/login";
        }
    }

    // ========================
    // ВЫХОД
    // ========================

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ========================
    // Главная страница
    // ========================

    @GetMapping("/")
    public String home(HttpSession session) {
        if  (session.getAttribute("currentUser") != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
}