package com.edison.project.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    // 블랙리스트에 토큰 추가 (TTL 설정)
    public void addToBlacklist(String token, long ttlInMillis) {
        redisTemplate.opsForValue().set(token, "logged_out", ttlInMillis, TimeUnit.MILLISECONDS);
    }

    // 토큰이 블랙리스트에 있는지 확인
    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(token);
    }
}
