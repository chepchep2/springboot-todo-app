package com.chep.demo.todo.service.invitation.producer;

import com.chep.demo.todo.service.invitation.RedisKeys;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class InvitationQueueProducer {
    private final RedisTemplate<String, String> redisTemplate;

    public InvitationQueueProducer(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void push(Long invitationId) {
        redisTemplate.opsForList().rightPush(RedisKeys.INVITATION_QUEUE, invitationId.toString());
    }
}
