package com.monitoring.tasps;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.monitoring.utilities.DataflowBean;

public class TaspsStatusWatchTask implements Runnable {
	private DataflowBean dataflow;
	static Logger log = Logger.getLogger(TaspsStatusWatchTask.class.getName());


	public TaspsStatusWatchTask(DataflowBean dataflow) {
		this.dataflow = dataflow;
	}

	@Override
	public void run() {

		TaspsStatusWatch TaspsStatusObject = null;
		try {
			TaspsStatusObject = new TaspsStatusWatch(dataflow);
		} catch (IOException e1) {
			log.error(e1);
		}

		
	}

}
