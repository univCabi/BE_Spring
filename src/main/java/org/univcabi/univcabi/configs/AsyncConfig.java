package org.univcabi.univcabi.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "cabinetTaskExecutor")
    public Executor cabinetTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cabinet-async-");

        // 작업 거부 정책: 호출자 스레드에서 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 아이들 스레드 제거 정책: 30초 동안 사용되지 않으면 제거
        executor.setKeepAliveSeconds(30);
        executor.setAllowCoreThreadTimeOut(true);

        executor.initialize();
        return executor;
    }

    @Bean(name = "redisSubscriptionExecutor")
    public TaskExecutor redisSubscriptionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("redis-sub-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "redisTaskExecutor")
    public TaskExecutor redisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("redis-task-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "kafkaProducerExecutor")
    public Executor kafkaProducerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100); // 메시지 작업은 많을 수 있으므로 큰 큐 할당
        executor.setThreadNamePrefix("kafka-prod-");
        executor.initialize();
        return executor;
    }
}
