package com.example.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String ACTIVE_USERS_KEY = "chat:active:users";

    public void addActiveUser(String username){
        redisTemplate.opsForSet().add(ACTIVE_USERS_KEY, username);
        System.out.println("Added to redis active user: " + username);
    }

    public void removeActiveUser(String username){
        redisTemplate.opsForSet().remove(ACTIVE_USERS_KEY, username);
        System.out.println("Removed from redis active users: " + username);
    }

    public boolean isUserActive(String username){
        Boolean isMember = redisTemplate.opsForSet().isMember(ACTIVE_USERS_KEY, username);
        return isMember != null && isMember;
    }

    public Set<Object> getActiveUsers(){
        return redisTemplate.opsForSet().members(ACTIVE_USERS_KEY);
    }

    public Long getActiveUserCount(){
        return redisTemplate.opsForSet().size(ACTIVE_USERS_KEY);
    }



}
