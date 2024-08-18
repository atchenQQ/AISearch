package com.atchen.AISearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-14
 * @Description: 创建线程池
 * @Version: 1.0
 */

@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors()+1);
        //最大线程数
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors()*2+1);
        // 任务队列容量
        executor.setQueueCapacity(10000);
        //线程拒绝策略
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                // 1 保存任务   // todo
                // 2 通知监控中心处理
            }
        });
        executor.initialize();
        return  executor  ;
    }
}
    