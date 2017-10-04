package com.monitoring.testmode;

import java.io.IOException;

import org.apache.log4j.Logger;

public class TestModeWatchTask implements Runnable {
	static Logger log = Logger.getLogger(TestModeWatchTask.class.getName());


	public TestModeWatchTask() {
	}

	@Override
	public void run() {

		TestModeWatch TaspsStatusObject = null;
		try {
			TaspsStatusObject = new TestModeWatch();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error(e1);
		}
	}

}
