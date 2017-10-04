package com.monitoring;

import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.monitoring.retrieving.RetrieveInfoWatchTask;
import com.monitoring.testmode.TestModeWatchTask;
import com.monitoring.trigger.TriggerTask;
import com.monitoring.utilities.AppConfig;
import com.monitoring.utilities.LoadProperties;
import com.monitoring.utilities.QueuesManager;

public class App {
	static Logger log = Logger.getLogger(App.class.getName());
	

	private static ThreadPoolTaskExecutor taskExecutor = null;

	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		new QueuesManager();
		taskExecutor = (ThreadPoolTaskExecutor) context.getBean("taskExecutor");
		taskExecutor.setKeepAliveSeconds(120000);
		if (LoadProperties.getProperty("TEST_MODE").equals("enabled")) {
			TestModeWatchTask mainTask = new TestModeWatchTask();
			Future<?> futureTask = taskExecutor.submit(mainTask);
			//taskExecutor.execute(mainTask);
		} else {
			// Start main task to run monitor over folder or dependencies
			RetrieveInfoWatchTask mainTask = new RetrieveInfoWatchTask(taskExecutor);
			// Start trigger task to set ready status of queued dataflow
			TriggerTask triggerTask = new TriggerTask();

			taskExecutor.execute(triggerTask);
			taskExecutor.execute(mainTask);
		}
		
		while (true){
			log.info("STAT: Number of Tasks currently in execution - " + taskExecutor.getActiveCount() + ". Current PoolSize :"+taskExecutor.getPoolSize());
			try {
				Thread.sleep(3600000);
			} catch (InterruptedException e) {
				log.debug("main interrupt" ,e);
			}
		}
	}
	
	
	public static ThreadPoolTaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public static void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		App.taskExecutor = taskExecutor;
	}

}
