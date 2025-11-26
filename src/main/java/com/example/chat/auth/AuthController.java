package com.example.chat.auth;

import com.example.chat.entity.LoginRequest;
import com.example.chat.security.JwtService;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    private final Map<String, String> users = Map.of(
        "admin", "admin123",
        "user", "user123"
    );

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request){
        String username = request.getUsername();
        String password = request.getPassword();

        if (!users.containsKey(username)){
            return ResponseEntity.status(401).body("User not found");
        }

        String correctPassword = users.get(username);
        if (!correctPassword.equals(password)){
            return ResponseEntity.status(401).body("Wrong password");
        }

        String token = jwtService.generateToken(username);
        System.out.println("Login successful. Token created for: " + username);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("username", username);
        response.put("message", "Login successful");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<?> test(@RequestHeader(value = "Authorization", required = false) String authHeader ){
        if (authHeader == null || !authHeader.startsWith("Bearer ")){
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
