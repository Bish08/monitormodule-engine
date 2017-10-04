package com.monitoring.trigger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.monitoring.retrieving.RetrieveInfoWatch;
import com.monitoring.utilities.CustomConstants;
import com.monitoring.utilities.DataflowBean;
import com.monitoring.utilities.LoadProperties;
import com.monitoring.utilities.MonitoringUtilities;
import com.monitoring.utilities.QueuesManager;

/**
 */

public class Trigger {

	static Logger log = Logger.getLogger(Trigger.class.getName());
	String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
	
	/**
	 * Create Trigger, start processEvents method
	 */
	public Trigger() throws IOException {
		MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), LoadProperties.getProperty(CustomConstants.NO_DATAFLOW), LoadProperties.getProperty(CustomConstants.NO_DATAFLOW));
		log.info("Started task responsible to trigger analysis.");
		try {
			processEvents();
		}  catch (Throwable e) {
		    log.error("Uncaught exception, Trigger Task has been stopped. Restart the MonitorModule", e); 
		}
	}

	/**
	 * Trigger Business Logic, set to In progress dataflows.
	 */
	void processEvents() {
		for (;;) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				log.debug("Interrupted Trigger", e1);
			}
			log.debug("Trigger Task - Waiting something to trigger"+"QueuesManager size "+ QueuesManager.getQueueDataflowCompleted().size());
			
			DataflowBean dataflow = null;
			if (QueuesManager.getQueueDataflowCompleted().size() != 0) {
				dataflow = QueuesManager.getQueueDataflowCompleted().poll();
				HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
				bodyRequest.put("tasps_id", dataflow.getTasps_id());
				bodyRequest.put("status", CustomConstants.STATUS_INPROGRESS);
				
				JsonObject response = MonitoringUtilities.updateStatus(urlString, bodyRequest);
				log.debug(response.toString());
				if (checkResponse(response,dataflow)){
					MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_CHECK_DEPENDECIES_TRIGGERED), dataflow.getDataflow_id(), dataflow.getTasps_id());
					log.info("Send request to update status to in progress and trigger job on TASPS for analysis with all dataflow dependencies completed in "
							+ dataflow.getValue_pack()+" value pack and dataflow id: "+dataflow.getDataflow_id()+" and TASPS id: "+dataflow.getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dataflow.getDataflow_load_date());
					
				}
				// RetrieveInfoWatch.getPendingDataflow().remove(dataflow.getId());
				List<Long> listThreadId = new ArrayList<Long>(RetrieveInfoWatch.mapThread.keySet());
				if (RetrieveInfoWatch.mapThread.get(dataflow.getId()) != null) {
					boolean stopped = RetrieveInfoWatch.mapThread.get(dataflow.getId()).cancel(true);
				}
				RetrieveInfoWatch.mapThread.remove(dataflow.getId());
			}
			if (QueuesManager.getQueueFileAvailable().size() != 0) {
				dataflow = QueuesManager.getQueueFileAvailable().poll();
				urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
				HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
				bodyRequest.put("tasps_id", dataflow.getTasps_id());
				bodyRequest.put("status", CustomConstants.STATUS_INPROGRESS);
				
				JsonObject response = MonitoringUtilities.updateStatus(urlString, bodyRequest);
				
				checkResponse(response,dataflow);
				log.debug(response.toString());
				if (checkResponse(response,dataflow)){
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_FILES_AV_TRIGGERED), dataflow.getDataflow_id(), dataflow.getTasps_id());
				log.info("Send request to update status to in progress and trigger job on TASPS for analysis with all files available in "
						+ dataflow.getValue_pack()+" value pack and dataflow id: "+dataflow.getDataflow_id()+" and TASPS id: "+dataflow.getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dataflow.getDataflow_load_date());
				}
				// RetrieveInfoWatch.getPendingDataflow().remove(dataflow.getId());
				List<Long> listThreadId = new ArrayList<Long>(RetrieveInfoWatch.mapThread.keySet());
				if (RetrieveInfoWatch.mapThread.get(dataflow.getId()) != null) {
					boolean stopped = RetrieveInfoWatch.mapThread.get(dataflow.getId()).cancel(true);
				}
				RetrieveInfoWatch.mapThread.remove(dataflow.getId());

			}

		}
	}
	
	public boolean checkResponse (JsonObject response, DataflowBean dataflow){
		if (response.get("result")==null || response.get("result").equals("401")){
			MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_FILES_AV_TRIGGERED), dataflow.getDataflow_id(), dataflow.getTasps_id());
			log.info("Error sending request to update status to in progress and trigger job on TASPS for analysis in "
					+ dataflow.getValue_pack()+" value pack and dataflow id: "+dataflow.getDataflow_id()+" and TASPS id: "+dataflow.getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dataflow.getDataflow_load_date() +". Error description: "+response.get("description"));
			HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
			bodyRequest.put("tasps_id", dataflow.getTasps_id());
			bodyRequest.put("status", CustomConstants.STATUS_DELETE);
			
			JsonObject responseError = MonitoringUtilities.updateStatus(urlString, bodyRequest);
			return false;
		} else {
			return true;
		}
	}

}
