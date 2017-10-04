package com.monitoring.retrieving;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class RetrieveInfoWatchTask implements Runnable {
	private ThreadPoolTaskExecutor taskExecutor;
	static Logger log = Logger.getLogger(RetrieveInfoWatchTask.class.getName());


	public RetrieveInfoWatchTask(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void run() {
		RetrieveInfoWatch retWatchObject = null;
		retWatchObject = new RetrieveInfoWatch(taskExecutor);

	}

}
