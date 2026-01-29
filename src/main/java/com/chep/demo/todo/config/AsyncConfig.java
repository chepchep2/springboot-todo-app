package com.chep.demo.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    private static final int MAIL_CORE_POOL_SIZE = 8;
    private static final int MAIL_MAX_POOL_SIZE = 16;
    private static final int MAIL_QUEUE_CAPACITY = 500;

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(MAIL_CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAIL_MAX_POOL_SIZE);
        executor.setQueueCapacity(MAIL_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return mailExecutor();
    }
}
