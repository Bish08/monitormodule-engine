package com.monitoring.dependencies;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.monitoring.retrieving.RetrieveInfoWatchTask;
import com.monitoring.utilities.DataflowBean;

public class DataflowDependenciesWatchTask implements Runnable {
	private DataflowBean dataflow;
	static Logger log = Logger.getLogger(DataflowDependenciesWatchTask.class.getName());


	public DataflowDependenciesWatchTask(DataflowBean dataflow) {
		this.dataflow = dataflow;
	}

	@Override
	public void run() {
		DataflowDependenciesWatch dataflowDependenciesObject = null;
		try {
			dataflowDependenciesObject = new DataflowDependenciesWatch(dataflow);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error(e1);
		}

	}

}
