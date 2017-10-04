package com.monitoring.retrieving;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.monitoring.dependencies.DataflowDependenciesWatchTask;
import com.monitoring.folder.DirectoryWatchTask;
import com.monitoring.tasps.TaspsStatusWatchTask;
import com.monitoring.utilities.CustomConstants;
import com.monitoring.utilities.DataflowBean;
import com.monitoring.utilities.LoadProperties;
import com.monitoring.utilities.MonitoringUtilities;
import com.monitoring.utilities.QueuesManager;

/**
 * TEST THIS CLASS AND SUBSTITUTE TO THE PREVIOUS ONE
 */

public class RetrieveInfoWatch {
	private static HashMap<Long, DataflowBean> pendingDataflow = new HashMap<Long, DataflowBean>();

	static Logger log = Logger.getLogger(RetrieveInfoWatch.class.getName());
	// private Dataflow dataflow;
	private ThreadPoolTaskExecutor taskExecutor;
	public static HashMap<Long, Future> mapThread = new HashMap<Long, Future>();

	/**
	 * 
	 * @return
	 * @throws FileNotFoundException
	 */
	public RetrieveInfoWatch(ThreadPoolTaskExecutor taskExecutor) {
		MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), LoadProperties.getProperty(CustomConstants.NO_DATAFLOW), LoadProperties.getProperty(CustomConstants.NO_DATAFLOW));
		log.info("Started Main Task Monitor Module");
		this.taskExecutor = taskExecutor;
		try {
			processEvents();
		}  catch (Throwable e) {
		    log.error("Uncaught exception, Main Task has been stopped. Restart the MonitorModule", e); 
		}
	}

	/**
	 * MAIN METHOD - Main Task
	 */
	
	void processEvents() {
		boolean continueSearch = true;
	
		while (continueSearch) {
			//RETRIEVE ALL ANALYSIS
			ArrayList<DataflowBean> dfArray = getAllAnalysis();
			//PING MONITOR IS RUNNING
			pingRunningMonitorModule();
			//RETRIEVE ALL VALUE PACK
			JsonArray vpArray = valuePacksArray();
			// CHECK and STOP DELETED ANALYSIS
			checkAndStopDeletedAnalysis(dfArray);
	
			for (int i = 0; i < dfArray.size(); i++) {
				String vpLoadDate = valuePackLoadDate(vpArray, dfArray,i);
				//PENDING DATAFLOW contains all analysis in monitoring / progress.
				DataflowBean analyzedDataflow = pendingDataflow.get(dfArray.get(i).getId());
				if (pendingDataflow.size() != 0 && analyzedDataflow != null
						&& dfArray.get(i).getTasps_id() != null) {
					//CHECK if DATAFLOW STATUS DELETED
					boolean continueFor = checkDeletedStatus(dfArray, i);
					if (continueFor){
						continue;
					}
					//CHECK if DATAFLOW MANUALLY TRIGGERED
					continueFor = checkManualTrigger(dfArray, i, analyzedDataflow);
					if (continueFor){
						continue;
					}
					//CHECK if DATAFLOW CHANGES UPDATE
					continueFor = checkDataflowChanges(dfArray, i, analyzedDataflow);
					if (continueFor){
						continue;
					}
					
				}
				
				// check if dataflow is in with status complete or error, eventually stop current task and auto set status to waiting
				checkStatusCompleteError(dfArray, i);

				// Start Monitor TASPS task if dataflow status is in progress
				startMonitorForAnalysisInProgress(dfArray, i);

				// Prepare Load Date values
				Date today = new Date();
				Date load_date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat(CustomConstants.DB_DATEONLY_FORMAT);
		
				try {
					load_date = sdf.parse(dfArray.get(i).getDataflow_load_date());

				} catch (ParseException e1) {
					log.error(e1);
				}
				
				// check if dataflow status must be set to Waiting (for tomorrow)
				boolean checkToWaiting = checkWaitingTomorrow(dfArray, i, vpLoadDate);
				log.info("   dataflow_date:"+dfArray.get(i).getDataflow_load_date()+"   tasps_id :"+dfArray.get(i).getTasps_id()+"  dataflwo_id :"+dfArray.get(i).getDataflow_id());
				
				// Start thread on monitoring (also restart monitoring in cause of crash) or scheduled (to be finished) 
				if(!pendingDataflow.containsKey(dfArray.get(i).getId()) && dfArray.get(i).getTasps_id() != null
						&& dfArray.get(i).getFrequency()== CustomConstants.FREQUENCY_HOURLY
						&& (dfArray.get(i).getStatus() == CustomConstants.STATUS_WAITING
						|| dfArray.get(i).getStatus() == CustomConstants.STATUS_MONITORING
						|| dfArray.get(i).getStatus() == CustomConstants.STATUS_SCHEDULED
						|| dfArray.get(i).getStatus() == CustomConstants.STATUS_HOURLY_COMPLETE
						|| dfArray.get(i).getStatus() == CustomConstants.STATUS_COMPLETE)
				&& today.after(load_date) && !checkToWaiting) 
				{
					log.info(dfArray.get(i).getDataflow_id()+"inside the if conditon line number 135 : if dataflow need of file availability start thread to monitor a folder");
					// if dataflow need of file availability start thread to monitor a folder
					//Added control for analysis in Queue to be not triggered 6/17
					startMonitorFileAvailability(dfArray, i);
					
					// once that task is started, set to monitoring the status.
					//setStatusToMonitoring(dfArray, i);
				}
				else if (!pendingDataflow.containsKey(dfArray.get(i).getId()) && dfArray.get(i).getTasps_id() != null
						&& (dfArray.get(i).getStatus() == CustomConstants.STATUS_WAITING
								|| dfArray.get(i).getStatus() == CustomConstants.STATUS_MONITORING
								|| dfArray.get(i).getStatus() == CustomConstants.STATUS_SCHEDULED)
						&& today.after(load_date) && !checkToWaiting && checkHistory(dfArray.get(i).getTasps_id(),vpLoadDate)!=true) {
				
					// if dataflow need of file availability start thread to monitor a folder
					//Added control for analysis in Queue to be not triggered 6/17
					log.info(dfArray.get(i).getDataflow_id()+"inside the if conditon line number 150 : if dataflow need of file availability start thread to monitor a folder");
					startMonitorFileAvailability(dfArray, i);
					
					// if dataflow need of dependencies start thread to monitor status
					//Added control for analysis in Queue to be not triggered 6/17
					startMonitorDataflowDependencies(dfArray, i);
					
					// if no trigger_type SCHEDULED start task to retrieve info about status. TODO
					startMonitorScheduledDataflow(dfArray, i);

					// once that task is started, set to monitoring the status.
					setStatusToMonitoring(dfArray, i);
				}
			}

			DataflowBean dataflowF = QueuesManager.getQueueFileAvailable().peek();
			DataflowBean dataflowD = QueuesManager.getQueueDataflowCompleted().peek();
			
			if (dataflowF != null){
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), dataflowF.getDataflow_id(), dataflowF.getTasps_id());
				log.info("One analysis available to be triggered for file availability in " + dataflowF.getValue_pack()+" value pack with dataflow id: " + dataflowF.getDataflow_id() + ". Relative TASPS id: " + dataflowF.getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dataflowF.getDataflow_load_date());
			}		
			if (dataflowD != null){
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), dataflowD.getDataflow_id(), dataflowD.getTasps_id());
				log.info("One analysis available to be triggered for all dependencies analysis completed in " + dataflowD.getValue_pack()+" value pack with dataflow id: " + dataflowD.getDataflow_id() + ". Relative TASPS id: " + dataflowD.getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dataflowD.getDataflow_load_date());
			}
			try {
				Thread.sleep(Integer.parseInt(LoadProperties.getProperty("MAIN_THREAD_POLLING_WAIT")));
			} catch (InterruptedException e) {
				log.error(e);
			}

		}
	}

	/******Main Task Utilities Methods*******/
	public void startMonitorFileAvailability (ArrayList<DataflowBean> dfArray, int i ){
		if (dfArray.get(i).getTrigger_type() == 1 && !QueuesManager.getQueueFileAvailable().contains(dfArray.get(i))) {
			DirectoryWatchTask dwTask = new DirectoryWatchTask(dfArray.get(i));
			pendingDataflow.put(dfArray.get(i).getId(), dfArray.get(i));
			Future taskRun = taskExecutor.submit(dwTask);
			this.mapThread.put(dfArray.get(i).getId(), taskRun);
			// set value to 1 , task started
			log.debug("Dataflow " + dfArray.get(i).getId() + " added to pendingDataflow HashMap");
			MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.START_MONITORING), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
			
			log.info("Task to monitor folder started for analysis in " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());

		}
	}
	
	public void startMonitorScheduledDataflow (ArrayList<DataflowBean> dfArray, int i ){
		if (dfArray.get(i).getTrigger_type() == 0) {
			TaspsStatusWatchTask dwTask = new TaspsStatusWatchTask(dfArray.get(i));
			Future taskRun = taskExecutor.submit(dwTask);
			this.mapThread.put(dfArray.get(i).getId(), taskRun);
			pendingDataflow.put(dfArray.get(i).getId(), dfArray.get(i));
			log.info("Task started to check status for scheduled analysis in " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
		}
	}
	public void startMonitorDataflowDependencies (ArrayList<DataflowBean> dfArray, int i){
		if (dfArray.get(i).getTrigger_type() == 2 && !QueuesManager.getQueueDataflowCompleted().contains(dfArray.get(i))) {
			
			log.info(dfArray.get(i).getDataflow_id()+" inside startMonitorDataflowDependencies");
			
			DataflowDependenciesWatchTask dwTask = new DataflowDependenciesWatchTask(dfArray.get(i));
			
			pendingDataflow.put(dfArray.get(i).getId(), dfArray.get(i));
			Future taskRun = taskExecutor.submit(dwTask);
			this.mapThread.put(dfArray.get(i).getId(), taskRun);

			log.debug("Dataflow " + dfArray.get(i).getId() + " added to pendingDataflow HashMap");
			MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.START_MONITORING), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
			log.info("Task to monitor analysis dependencies started for analysis in " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());

		}
	}
	
	public void setStatusToMonitoring(ArrayList<DataflowBean> dfArray, int i){
		if (mapThread.containsKey(dfArray.get(i).getId())) {
			String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
			HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
			bodyRequest.put("tasps_id", dfArray.get(i).getTasps_id());
			bodyRequest.put("status", CustomConstants.STATUS_MONITORING);
			MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.START_MONITORING), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
			log.info("Status to monitoring for analysis in " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
			MonitoringUtilities.updateStatus(urlString, bodyRequest);
			if (pendingDataflow.get(dfArray.get(i).getId()) != null) {
				pendingDataflow.get(dfArray.get(i).getId()).setStatus(CustomConstants.STATUS_MONITORING);
			}

		}
	}
	public static HashMap<Long, DataflowBean> getPendingDataflow() {
		return pendingDataflow;
	}

	public static void setPendingDataflow(HashMap<Long, DataflowBean> pendingDataflow) {
		RetrieveInfoWatch.pendingDataflow = pendingDataflow;
	}

	public static HashMap<Long, Future> getMapThread() {
		return mapThread;
	}

	public static void setMapThread(HashMap<Long, Future> mapThread) {
		RetrieveInfoWatch.mapThread = mapThread;
	}
	
	
	public static boolean checkHistory (String tasps_id, String dataflow_load_date){
		CloseableHttpClient httpClient = HttpClients.createDefault();
		CloseableHttpResponse httpResponse = null;
		String responseBody = "";
		JsonArray listOfDataflow = null;

		HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
		bodyRequest.put("tasps_id", tasps_id);
		bodyRequest.put("dataflow_load_date", dataflow_load_date);
		httpResponse = MonitoringUtilities.sendPost(httpClient, new Gson().toJson(bodyRequest),
				LoadProperties.getProperty("API_GET_BY_TASPS_AND_STATUS"));
		try {
			responseBody = EntityUtils.toString(httpResponse.getEntity());
		} catch (IOException e1) {
			log.error(e1);
		} finally {
			try {
				httpResponse.close();
				httpClient.close();
			} catch (IOException e) {
				log.error("ERROR", e);
			}
		}
		JsonParser jsonParser = new JsonParser();
		listOfDataflow = (JsonArray) jsonParser.parse(responseBody);

		if (listOfDataflow.getAsJsonArray().size() != 0) {
			for (int j = 0; j < listOfDataflow.getAsJsonArray().size(); j++) {

				if (Integer.parseInt(listOfDataflow.getAsJsonArray().get(j).getAsJsonObject().get("status")
						.toString()) == CustomConstants.STATUS_COMPLETE) {
					return true;
					
				}
			}
		}
		return false;
	}
	
	private JsonArray valuePacksArray(){
		String responseBodyValuePack = "";
		CloseableHttpClient httpClientValuePack = HttpClients.createDefault();
		CloseableHttpResponse httpResponseValuePack = null;
		String url = LoadProperties.getProperty("API_GET_ALL_VALUE_PACK") ;
		httpResponseValuePack = MonitoringUtilities.sendGetWithoutClose(httpClientValuePack, url, null);
		try {
			responseBodyValuePack = EntityUtils.toString(httpResponseValuePack.getEntity());
		} catch (IOException e1) {
			log.error(e1);
		} finally {
			try {
				httpResponseValuePack.close();
				httpClientValuePack.close();
			} catch (IOException e) {
				log.error("ERROR", e);
			}
		}
		JsonParser jsonParser = new JsonParser();
		
		JsonArray vpArray = jsonParser.parse(responseBodyValuePack).getAsJsonArray();
		return vpArray;
	}
	
	public ArrayList<DataflowBean> getAllAnalysis(){
		ArrayList<DataflowBean> dfArray = null;
		//RETRIEVE ANALYSIS
		try {
			dfArray = new ArrayList<DataflowBean>(Arrays.asList(
					MonitoringUtilities.retrieveAllDataFlow(new URL(LoadProperties.getProperty("API_GET_ALL")))));
			log.trace("Check if there are other dataflows in waiting status - API_GET_ALL called");
			log.trace("pending in monitormodule " +pendingDataflow.toString());
			log.trace("dataflows response " + dfArray.toString());
		} catch (MalformedURLException e1) {
			log.error(e1);
		}
		return dfArray;
	}
	
	public void pingRunningMonitorModule (){
		// PING I'm an Hardworker module and I'm still working!
					HashMap<String, Object> bodyRequestPing = new HashMap<String, Object>();
					bodyRequestPing.put("module", "MonitorModule");
					CloseableHttpResponse httpResponse = null;
					CloseableHttpClient httpClient = HttpClients.createDefault();
					httpResponse = MonitoringUtilities.sendPost(httpClient,new Gson().toJson(bodyRequestPing),
							LoadProperties.getProperty("API_UPDATE_STATUS_TABLE"));
					try {
						httpClient.close();
					} catch (IOException e2) {
						log.error("ERROR",e2);
					}
	}
	
	public void checkAndStopDeletedAnalysis (ArrayList<DataflowBean> dfArray){
		List<Long> listThreadId = new ArrayList<Long>(this.mapThread.keySet());
		List<Long> listDataflowIdonTable = new ArrayList<Long>();
		for (int i = 0; i < dfArray.size(); i++) {
			listDataflowIdonTable.add(dfArray.get(i).getId());
		}
		
		for (int i = 0; i < listThreadId.size(); i++) {
			if (!listDataflowIdonTable.contains(listThreadId.get(i))) {
				boolean stopped = this.mapThread.get(listThreadId.get(i)).cancel(true);
				if (stopped) {
					this.mapThread.remove(listThreadId.get(i));
					MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.STOP_MONITORING), "N/A", "N/A");
					log.info("Dataflow deleted. For this reason monitor thread is stopped for id "
							+ listThreadId.get(i));
				}
			}

		}
		// end delete (data cancelled from db)
	}
	
	public String valuePackLoadDate(JsonArray vpArray, ArrayList<DataflowBean> dfArray, int i ){
		String vpLoadDate = "";
		for (int j=0; j< vpArray.size(); j++){
			if (dfArray.get(i).getValue_pack().equals(vpArray.get(j).getAsJsonObject().get("dataflow_id").toString().replaceAll("\"", "")))
				vpLoadDate = vpArray.get(j).getAsJsonObject().get("dataflow_load_date").toString().replaceAll("\"", "");
				//log.trace("dataflow_load_date value pack retrieved: "+vpLoadDate);
		}
		return vpLoadDate;
	}

	public boolean checkDeletedStatus (ArrayList<DataflowBean> dfArray, int i){
		// check deleted dataflow in status delete 5
		if (dfArray.get(i).getStatus() == CustomConstants.STATUS_DELETE && this.mapThread.containsKey(dfArray.get(i).getId())) {
			boolean stopped = this.mapThread.get(dfArray.get(i).getId()).cancel(true);
			pendingDataflow.remove(dfArray.get(i).getId());
			log.debug("Status deleted found "+dfArray.get(i).getId());
			if (stopped) {
				this.mapThread.remove(dfArray.get(i).getId());
				pendingDataflow.remove(dfArray.get(i).getId());
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.STOP_MONITORING), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
				log.info(
						"Analysis monitor stopped. Status deleted for analysis of " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
				return true;
			}	
		}
		return false;
	}
	
	public boolean checkManualTrigger (ArrayList<DataflowBean> dfArray, int i,DataflowBean analyzedDataflow){
		// check manual trigger in this.mapThread
		if (analyzedDataflow != null && analyzedDataflow.getStatus() != CustomConstants.STATUS_INPROGRESS
				&& dfArray.get(i).getStatus() == CustomConstants.STATUS_INPROGRESS
				&& this.mapThread.containsKey(dfArray.get(i).getId())) {
			boolean stopped = this.mapThread.get(dfArray.get(i).getId()).cancel(true);
			// deleted if stopped because seems to be already
			// stopped.
			MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_MANUALLY_TRIGGERED), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
			log.info("Run has been manually triggered for analysis of " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
			this.mapThread.remove(dfArray.get(i).getId());
			pendingDataflow.remove(dfArray.get(i).getId());
			return true;
			}
		return false;
	}
	
	public boolean checkDataflowChanges (ArrayList<DataflowBean> dfArray, int i,DataflowBean analyzedDataflow){
		// check realtime update on db entry TODO Check if must be
		// removed something from equals override dataflow
		if (analyzedDataflow != null && !analyzedDataflow.equals(dfArray.get(i)) ) { 
			if (this.mapThread.get(dfArray.get(i).getId()) != null) {
				boolean stopped = this.mapThread.get(dfArray.get(i).getId()).cancel(true);
				this.mapThread.remove(dfArray.get(i).getId());
				DataflowBean removedDataflow = analyzedDataflow;
				pendingDataflow.remove(dfArray.get(i).getId());
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.EDIT_LOAD_DATE), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
				
				if (!dfArray.get(i).getStatus().equals(removedDataflow.getStatus())){
					log.info("Analysis monitor stopped. Status changed from "+removedDataflow.getStatus()+" to "+dfArray.get(i).getStatus()+ " for analysis of " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());	
				}else {
				log.info("Analysis monitor stopped. Info modified for analysis of " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
				}
					return true;
			}
		}
		return false;

	}
	
	public void checkStatusCompleteError (ArrayList<DataflowBean> dfArray, int i){
		if ((dfArray.get(i).getStatus() == CustomConstants.STATUS_ERROR
				|| dfArray.get(i).getStatus() == CustomConstants.STATUS_COMPLETE)) {
			//Add Thread.sleep // Avoid concurrent update / dataflow_load_date replace
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				log.error("Waiting Thread interrupted after Status Complete Retrieved.");
			}
			if (this.mapThread.containsKey(dfArray.get(i).getId())) {

				boolean stopped = this.mapThread.get(dfArray.get(i).getId()).cancel(true);
				this.mapThread.remove(dfArray.get(i).getId());
				pendingDataflow.remove(dfArray.get(i).getId());
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.STOP_MONITORING), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
				log.info("Task to retrieve status from TASPS stopped. Analysis completed on TASPS for analysis in " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
			}
			if (dfArray.get(i).getTasps_id() != null
					&& dfArray.get(i).getStatus() == CustomConstants.STATUS_COMPLETE) {
				// if status is completed se to scheduled or waiting
				// according to the trigger type.
				String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
				HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
				bodyRequest.put("tasps_id", dfArray.get(i).getTasps_id());
				if (dfArray.get(i).getTrigger_type() == 0) {
					bodyRequest.put("status", CustomConstants.STATUS_SCHEDULED);
				} else {
					bodyRequest.put("status", CustomConstants.STATUS_WAITING);
				}
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.STOP_MONITORING), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
					log.info("Status to scheduled/waiting for completed analysis in " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
				MonitoringUtilities.updateStatus(urlString, bodyRequest);

			}

		}
	}
	
	public void startMonitorForAnalysisInProgress (ArrayList<DataflowBean> dfArray, int i){
		if (dfArray.get(i).getTasps_id() != null
				&& dfArray.get(i).getStatus() == CustomConstants.STATUS_INPROGRESS
				&& !this.mapThread.containsKey(dfArray.get(i).getId())) {
			TaspsStatusWatchTask dwTask = new TaspsStatusWatchTask(dfArray.get(i));
			Future taskRun = taskExecutor.submit(dwTask);
			this.mapThread.put(dfArray.get(i).getId(), taskRun);
			pendingDataflow.put(dfArray.get(i).getId(), dfArray.get(i));
			MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.START_MONITORING), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
			log.info("Task to retrieve info on TASPS job status started for analysis in " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
		}
	}
	
	public boolean checkWaitingTomorrow (ArrayList<DataflowBean> dfArray, int i, String vpLoadDate){
		Date today = new Date();
		Date load_date = new Date();
		Date vp_load_date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(CustomConstants.DB_DATEONLY_FORMAT);

		try {
			load_date = sdf.parse(dfArray.get(i).getDataflow_load_date());
			vp_load_date = sdf.parse(vpLoadDate);

		} catch (ParseException e1) {
			log.error(e1);
		}
		
		boolean checkToWaiting = dfArray.get(i).getFrequency()==CustomConstants.FREQUENCY_HOURLY ? false : (dfArray.get(i).getLast_run_date().equals(sdf.format(today)) && dfArray.get(i).getFrequency()==0);
		
		log.info(dfArray.get(i).getDataflow_id()+"after the checking the CheckToWaiting: "+checkToWaiting);
		//boolean checkToWaiting = (dfArray.get(i).getLast_run_date().equals(sdf.format(today)) && dfArray.get(i).getFrequency()==0);
		// in waiting monitoring today / future analysis
		if (dfArray.get(i).getStatus() == CustomConstants.STATUS_MONITORING && checkToWaiting){
			String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
			HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
			bodyRequest.put("tasps_id", dfArray.get(i).getTasps_id());
			bodyRequest.put("status", CustomConstants.STATUS_WAITING);
			MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.STOP_MONITORING), dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
			log.info("Waiting tomorrow to monitor analysis in " + dfArray.get(i).getValue_pack()+" value pack with dataflow id: " + dfArray.get(i).getDataflow_id() + ". Relative TASPS id: " + dfArray.get(i).getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dfArray.get(i).getDataflow_load_date());
			MonitoringUtilities.updateStatus(urlString, bodyRequest);
			
		}
		return checkToWaiting;
	}
}
