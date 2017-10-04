package com.monitoring.utilities;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ComponentScan(basePackages = "com.monitoring")
public class AppConfig {

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
		pool.setCorePoolSize(Integer.parseInt(LoadProperties.getProperty("CORE_POOL_SIZE")));
		pool.setMaxPoolSize(Integer.parseInt(LoadProperties.getProperty("MAX_CORE_POOL_SIZE")));
		pool.setQueueCapacity(Integer.parseInt(LoadProperties.getProperty("QUEUE_CAPACITY")));
		pool.setWaitForTasksToCompleteOnShutdown(true);
		return pool;
	}

}