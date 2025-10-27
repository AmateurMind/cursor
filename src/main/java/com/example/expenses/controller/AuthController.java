package com.example.expenses.controller;

import com.example.expenses.model.User;
import com.example.expenses.repository.UserRepository;
import com.example.expenses.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = safe(body.get("email"));
        String name = safe(body.get("name"));
        String password = body.get("password");
        if (!StringUtils.hasText(email) || !StringUtils.hasText(name) || !StringUtils.hasText(password)) {
            return ResponseEntity.badRequest().body(Map.of("error", "email, name, password required"));
        }
        String hash = SecurityUtil.sha256(password);
        Long id = userRepository.createWithPassword(email, name, hash);
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("email", email);
        res.put("name", name);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = safe(body.get("email"));
        String password = body.get("password");
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and password required"));
        }
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        User u = userOpt.get();
        String hash = SecurityUtil.sha256(password);
        if (!hash.equals(u.getPasswordHash())) return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        // Simple session stub: return user info (client can store userId)
        Map<String, Object> res = new HashMap<>();
        res.put("id", u.getId());
        res.put("email", u.getEmail());
        res.put("name", u.getName());
        return ResponseEntity.ok(res);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}