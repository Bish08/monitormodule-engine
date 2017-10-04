package com.monitoring.folder;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.monitoring.retrieving.RetrieveInfoWatchTask;
import com.monitoring.utilities.DataflowBean;

public class DirectoryWatchTask implements Runnable {
	private DataflowBean dataflow;
	static Logger log = Logger.getLogger(DirectoryWatchTask.class.getName());


	public DirectoryWatchTask(DataflowBean dataflow) {
		this.dataflow = dataflow;
	}

	@Override
	public void run() {
		DirectoryWatch dirwatchObject = null;
		try {
			dirwatchObject = new DirectoryWatch(dataflow, "Y");
		} catch (IOException e1) {
			log.error(e1);
		}

	}

}
