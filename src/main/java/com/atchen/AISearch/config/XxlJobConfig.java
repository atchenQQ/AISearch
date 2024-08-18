package com.atchen.AISearch.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-15
 * @Description: xxl配置文件
 * @Version: 1.0
 */

@Configuration
public class XxlJobConfig {
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;
    @Value("${xxl.job.access-token}")
    private String accessToken;
    @Value("${xxl.job.executor.appname}")
    private String appName;
    @Value("${server.port}")
    private int port;
    @Value("${xxl.job.executor.logpath}")
    private String logPath;
    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appName);
        executor.setPort(port+20010);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor ;
    }

    @XxlJob("testJobHandler")
    public void testjob(){
        System.out.println("test job handler");
    }

}
    