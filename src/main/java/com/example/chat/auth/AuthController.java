package com.example.chat.auth;

import com.example.chat.entity.LoginRequest;
import com.example.chat.entity.User;
import com.example.chat.repo.UserRepository;
import com.example.chat.security.JwtService;
import com.example.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        try {
            System.out.println("=== LOGIN ATTEMPT ===");
            System.out.println("Username: " + request.getUsername());
            System.out.println("Password provided (length): " +
                    (request.getPassword() != null ? request.getPassword().length() : "null"));

            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                System.out.println("Username is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Username is required"));
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                System.out.println("Password is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Password is required"));
            }

            var userOptional = userRepository.findByUsername(request.getUsername());
            if (userOptional.isEmpty()) {
                System.out.println("User not found in database: " + request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid username or password"));
            }

            User user = userOptional.get();
            System.out.println("User found - ID: " + user.getId() + ", Username: " + user.getUsername());
            System.out.println("Stored hashed password: " + user.getPassword());

            String rawPassword = request.getPassword();
            String storedHashedPassword = user.getPassword();

            System.out.println("Attempting password match...");
            boolean passwordMatches = passwordEncoder.matches(rawPassword, storedHashedPassword);
            System.out.println("Password match result: " + passwordMatches);

            if (!passwordMatches) {
                System.out.println("Password does not match for user: " + request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid username or password"));
            }

            System.out.println("Generating JWT token...");
            String token;
            try {
                token = jwtService.generateToken(user.getUsername());
                System.out.println("Token generated successfully");
            } catch (Exception e) {
                System.out.println("JWT generation error: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Token generation failed"));
            }

            userService.addUsers(user.getUsername());
            System.out.println("Added user to Redis active users: " + user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("timestamp", new Date().toString());
            response.put("token", token);

            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", user.getId());
            userResponse.put("username", user.getUsername());
            response.put("user", userResponse);

            System.out.println("=== LOGIN SUCCESSFUL ===");
            System.out.println("Response being sent: " + response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("=== LOGIN ERROR ===");
            System.out.println("Exception type: " + e.getClass().getName());
            System.out.println("Exception message: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        try {
            System.out.println("Registration attempt for user: " + request.getUsername());

            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }

            if (request.getPassword().length() < 3) {
                return ResponseEntity.badRequest().body("Password must be at least 3 characters");
            }

            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            User newUser = new User();
            newUser.setUsername(request.getUsername().trim());

            String hashedPassword = passwordEncoder.encode(request.getPassword());
            newUser.setPassword(hashedPassword);
            newUser.setRole("USER");

            User savedUser = userRepository.save(newUser);
            System.out.println("User registered successfully with ID: " + savedUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", savedUser.getUsername());
            response.put("timestamp", new Date().toString());

            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", savedUser.getId());
            userResponse.put("username", savedUser.getUsername());
            response.put("user", userResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader){
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")){
                return ResponseEntity.badRequest().body("No valid valid provided");
            }

            String token = authHeader.substring(7);
            String username = jwtService.getUsernameFromToken(token);

            userService.removeUser(username);
            System.out.println("User removed from Redis active users: " + username);

            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            System.out.println("Logout error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Logout failed: " + e.getMessage());
        }
    }

    @GetMapping("/active-users")
    public ResponseEntity<?> getActiveUsers(){
        try {
            Map<String, Object> response = new HashMap<>();

            Set<String> activeUsers = userService.getActiveUsers();
            int count = userService.getActiveUserCount();

            response.put("activeUsers", activeUsers);
            response.put("count", count);
            response.put("timestamp", new Date());
            response.put("source", "Redis");

            System.out.println("Active users retrieved from Redis. Count: " + count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error getting active users: " + e.getMessage());
            return ResponseEntity.status(500).body("Error retrieving active users: " + e.getMessage());
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

    private Map<String, Object> createErrorResponse(String message){
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", new Date().toString());
        return errorResponse;
    }

}