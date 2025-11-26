package com.example.chat.chat;

import com.example.chat.security.JwtService;
import com.example.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class SecuredChatRestController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestHeader("Authorization") String authHeader, @RequestBody ChatMessage chatMessage){
        if (!authHeader.startsWith("Bearer ")){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token format");
        }
        String token = authHeader.substring(7);
        System.out.println("Token received");

        try {
            String username = jwtService.getUsernameFromToken(token);
            if (!jwtService.isTokenValid(token, username)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            System.out.println("Token valid for user: " + username);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token verification failed." + e.getMessage());
        }
        if (!userService.isActive(chatMessage.getSender())){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not active");
        }
        chatMessage.setType(MessageType.CHAT);
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
        System.out.println("Message sent: " + chatMessage.getContent());
        return ResponseEntity.ok(chatMessage);
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinChat(@RequestHeader("Authorization") String authHeader, @RequestParam String username){
        if (!authHeader.startsWith("Bearer ")){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token format");
        }
        String token = authHeader.substring(7);

        try {
            if (!jwtService.isTokenValid(token, username)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token verification failed." + e.getMessage());
        }
        userService.addUsers(username);

        ChatMessage chatMessage = ChatMessage.builder()
                .sender(username)
                .type(MessageType.JOIN)
                .build();
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
        System.out.println(username + " joined");
        return ResponseEntity.ok(chatMessage);
    }


    @PostMapping("/leave")
    public ResponseEntity<?> leaveChat(@RequestHeader("Authorization") String authHeader, @RequestParam String username){
        if (!authHeader.startsWith("Bearer ")){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token format");
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtService.isTokenValid(token, username)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token verification failed." + e.getMessage());
        }

        userService.removeUser(username);

        ChatMessage chatMessage = ChatMessage.builder()
                .sender(username)
                .type(MessageType.LEAVE)
                .build();
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
        System.out.println(username + " left");
        return ResponseEntity.ok(chatMessage);
    }

}
