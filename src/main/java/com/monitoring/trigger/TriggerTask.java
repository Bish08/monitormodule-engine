package com.monitoring.trigger;

import java.io.IOException;

import org.apache.log4j.Logger;

public class TriggerTask implements Runnable {
	static Logger log = Logger.getLogger(TriggerTask.class.getName());


	public TriggerTask() {
	}

	@Override
	public void run() {

		Trigger triggerObject = null;
		try {
			triggerObject = new Trigger();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error(e1);	
		}

		//triggerObject.processEvents();

	}

}
