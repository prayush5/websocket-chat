package com.example.chat.service;

import com.example.chat.entity.User;
import com.example.chat.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();


    public void addUsers(String username){
        activeUsers.add(username);
        System.out.println("User added to active list: " + username);
        System.out.println("Total active users: " + activeUsers.size());
    }

    public void removeUser(String username){
        activeUsers.remove(username);
        System.out.println("User" + username + "removed");
        System.out.println("Total active users: " + activeUsers.size());
    }

    public boolean isActive(String username){
        return activeUsers.contains(username);
    }

    public Set<String> getActiveUsers(){
        return Set.copyOf(activeUsers);
    }

    public int getActiveUserCount(){
        return activeUsers.size();
    }
}
