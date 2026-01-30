package com.chep.demo.todo.service.invitation.consumer;

import com.chep.demo.todo.service.invitation.RedisKeys;
import com.chep.demo.todo.service.invitation.processor.InvitationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class InvitationQueueConsumer {
    private static final Logger log = LoggerFactory.getLogger(InvitationQueueConsumer.class);
    private static final int BLOCKING_TIMEOUT_SECONDS = 5;
    private static final int SEND_INTERVAL_MS = 500;
    private final RedisTemplate<String, String> redisTemplate;
    private final InvitationProcessor invitationProcessor;

    public InvitationQueueConsumer(RedisTemplate<String, String> redisTemplate, InvitationProcessor invitationProcessor) {
        this.redisTemplate = redisTemplate;
        this.invitationProcessor = invitationProcessor;
    }

    public void startConsuming() {
        while (true) {
            try {
                String invitationId = redisTemplate.opsForList()
                        .leftPop(RedisKeys.INVITATION_QUEUE, BLOCKING_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (invitationId != null) {
                    Long id = Long.parseLong(invitationId);
                    invitationProcessor.process(id);
                    Thread.sleep(SEND_INTERVAL_MS);
                }
            } catch (Exception e) {
                log.error("Error consuming from queue", e);
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startOnApplicationReady() {
        new Thread(this::startConsuming).start();
    }
}
