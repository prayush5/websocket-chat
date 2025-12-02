package com.example.chat.service;

import com.example.chat.entity.User;
import com.example.chat.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
//    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();


    public void addUsers(String username){
        redisService.addActiveUser(username);
        printActiveUsers();
    }

    public void removeUser(String username){
        redisService.removeActiveUser(username);
        printActiveUsers();
    }

    public boolean isActive(String username){
        return redisService.isUserActive(username);
    }

    public Set<String> getActiveUsers(){
        Set<Object> redisUsers = redisService.getActiveUsers();
        return redisUsers.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    public int getActiveUserCount(){
        Long count = redisService.getActiveUserCount();
        return count != null ? count.intValue() : 0;
    }

    private void printActiveUsers(){
        System.out.println("=== Active Users (from redis) ===");
        System.out.println("Users: " + getActiveUsers());
        System.out.println("Count: " + getActiveUserCount());
        System.out.println("=================================");
    }
}
