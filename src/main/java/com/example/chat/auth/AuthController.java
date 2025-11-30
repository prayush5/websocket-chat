package com.example.chat.auth;

import com.example.chat.entity.LoginRequest;
import com.example.chat.entity.User;
import com.example.chat.repo.UserRepository;
import com.example.chat.security.JwtService;
import com.example.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println("Login attempt for user: " + request.getUsername());

        var userOptional = userRepository.findByUsername(request.getUsername());

        if (userOptional.isEmpty()) {
            System.out.println("User not found: " + request.getUsername());
            return ResponseEntity.status(401).body("User not found");
        }

        User user = userOptional.get();
        System.out.println("User found: " + user.getUsername() + ", stored password: " + user.getPassword());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            System.out.println("Password mismatch for user: " + request.getUsername());
            return ResponseEntity.status(401).body("Wrong password");
        }

        String token = jwtService.generateToken(user.getUsername());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername()); // Fixed: removed colon
        response.put("message", "Login successful");

        System.out.println("Login successful for user: " + user.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        try {
            System.out.println("Registration attempt for user: " + request.getUsername());

            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                System.out.println("Username already exists: " + request.getUsername());
                return ResponseEntity.badRequest().body("Username already exists");
            }

            String hashedPassword = passwordEncoder.encode(request.getPassword());
            User newUser = new User();
            newUser.setUsername(request.getUsername());
            newUser.setPassword(hashedPassword);
            newUser.setRole("USER");

            User savedUser = userRepository.save(newUser);
            System.out.println("User registered successfully with ID: " + savedUser.getId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("No token provided");
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.getUsernameFromToken(token);
            return ResponseEntity.ok("Hello " + username + ", your token is valid");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token");
        }
    }
}