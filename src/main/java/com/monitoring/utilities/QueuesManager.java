package com.monitoring.utilities;

import java.util.LinkedList;
import java.util.Queue;

public class QueuesManager {

	private static Queue<DataflowBean> queueFileAvailable;
	private static Queue<DataflowBean> queueDataflowDependencies;

	public QueuesManager() {
		QueuesManager.queueFileAvailable = new LinkedList<DataflowBean>();
		QueuesManager.queueDataflowDependencies = new LinkedList<DataflowBean>();
	}

	public static Queue<DataflowBean> getQueueDataflowCompleted() {
		return queueDataflowDependencies;
	}

	public static void setQueueDataflowCompleted(Queue<DataflowBean> queueDataflowCompleted) {
		QueuesManager.queueDataflowDependencies = queueDataflowCompleted;
	}

	public static Queue<DataflowBean> getQueueFileAvailable() {
		return queueFileAvailable;
	}

	public static void setQueueFileAvailable(Queue<DataflowBean> queueFileAvailable) {
		QueuesManager.queueFileAvailable = queueFileAvailable;
	}
}