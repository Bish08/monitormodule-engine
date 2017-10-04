package com.monitoring.tasps;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.monitoring.utilities.CustomConstants;
import com.monitoring.utilities.DataflowBean;
import com.monitoring.utilities.LoadProperties;
import com.monitoring.utilities.MonitoringUtilities;

/**
*
 */

public class TaspsStatusWatch {

	static Logger log = Logger.getLogger(TaspsStatusWatch.class.getName());

	private DataflowBean dataflow;

	/**
	 * Constructor that call method processEvents()
	 * 
	 * @return
	 */
	public TaspsStatusWatch(DataflowBean dataflow) throws IOException {

		log.debug("Started task to retrieve info from TASPS");

		this.dataflow = dataflow;
		try {
			processEvents();
		}  catch (Throwable e) {
		    log.error("Uncaught exception", e); 
		}
	}

	/**
	 * 
	 * 
	 * @throws InterruptedException
	 * 
	 */
	void processEvents() {
		
		boolean continueSearch = true;
		// log.debug("In process events Tasps Status");
		// while dataflow isn't sent to queue ready, continueSearch
		while (continueSearch) {
			//TASPS Engine slow to update status
			try {
				Thread.sleep(Integer.parseInt(LoadProperties.getProperty("THREAD_POLLING_WAIT")));
			} catch (InterruptedException e) {
				continueSearch=false;
				log.debug("Interrupted TASPS WATCH", e);
			}

			String urlRetrieveInfoTasps = LoadProperties.getProperty("API_RETRIEVE_STATUS_TASPS")
					+ dataflow.getTasps_id();
			List<String> roles = new ArrayList<String>();
			roles = MonitoringUtilities.addRoles(roles);
			// speadmin,speoper,spedesigner,speexpertdesigner,spehpdeveloper
			CloseableHttpClient httpClient = HttpClients.createDefault();

			String xAccessToken = MonitoringUtilities.mkJWT("user", roles);
			CloseableHttpResponse response = MonitoringUtilities.sendGetWithoutClose(httpClient,urlRetrieveInfoTasps, xAccessToken);
			String responseBody = "";
			try {
				responseBody = EntityUtils.toString(response.getEntity());
			} catch (ParseException e1) {

				log.error(e1);
			} catch (IOException e1) {

				log.error(e1);
			} finally {
				try {
					response.close();
					httpClient.close();
				} catch (IOException e) {
					log.error("ERROR", e);
				}
			}
			JsonParser jsonParser = new JsonParser();
			JsonObject jo = (JsonObject) jsonParser.parse(responseBody);
			String resultDescription = jo.get("rows").getAsJsonArray().get(0).getAsJsonObject().get("result")
					.toString();
			String status = jo.get("rows").getAsJsonArray().get(0).getAsJsonObject().get("status").toString();
			String runEndTime = jo.get("rows").getAsJsonArray().get(0).getAsJsonObject().get("runEndTime").toString();
			boolean checkLastRun = checkLastRun (runEndTime);
			boolean checkLastRunDataflow = checkDataflowLastRun(dataflow, runEndTime);
			String dataflowLastEndRunDate= dataflow.getLast_end_run_date()==null? "" : dataflow.getLast_end_run_date();

			if (Integer.parseInt(status) == CustomConstants.STATUS_TASPS_COMPLETE && checkLastRunDataflow && checkLastRun) {
				
				// set status ready on db using tasps_id
				String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
				HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
				bodyRequest.put("tasps_id", dataflow.getTasps_id());
				if(dataflow.getFrequency()==CustomConstants.FREQUENCY_HOURLY) {
					bodyRequest.put("status", CustomConstants.STATUS_HOURLY_COMPLETE);
					log.info("Updating the status in the dataflow_info_status table: "+ CustomConstants.STATUS_HOURLY_COMPLETE );
				}
				else
				{
				bodyRequest.put("status", CustomConstants.STATUS_COMPLETE);
				}
				bodyRequest.put("description_status", resultDescription);
				bodyRequest.put("last_end_run_date", runEndTime);
				JsonObject responseUpdate = MonitoringUtilities.updateStatus(urlString, bodyRequest);
				checkResponse (responseUpdate,dataflow);
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_COMPLETED), dataflow.getDataflow_id(), dataflow.getTasps_id());
				log.info("Status complete retrieved from TASPS for analysis in " + dataflow.getValue_pack()
						+ " value pack with dataflow id: " + dataflow.getDataflow_id() + " and TASPS id: "
						+ dataflow.getTasps_id() + ". SDM_DATAFLOW.dataflow_load_date="
						+ dataflow.getDataflow_load_date() + ". Last End Run Time returned by TASPS: "+ runEndTime + "previous Last End Run Time stored into SDM: " + dataflowLastEndRunDate);
				continueSearch = false;
				
			} else if ((Integer.parseInt(status) == CustomConstants.STATUS_TASPS_ERROR
					|| Integer.parseInt(status) == CustomConstants.STATUS_TASPS_ABORTED) && checkLastRunDataflow && checkLastRun) {
				String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
				HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
				bodyRequest.put("tasps_id", dataflow.getTasps_id());
				bodyRequest.put("status", CustomConstants.STATUS_ERROR);
				bodyRequest.put("description_status", resultDescription);
				bodyRequest.put("last_end_run_date", runEndTime);
				
				JsonObject responseUpdate = MonitoringUtilities.updateStatus(urlString, bodyRequest);
				checkResponse (responseUpdate,dataflow);
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_ERROR), dataflow.getDataflow_id(), dataflow.getTasps_id());
				
				log.info("Status error retrieved from TASPS for analysis in " + dataflow.getValue_pack()
						+ " value pack with dataflow id: " + dataflow.getDataflow_id() + " and TASPS id: "
						+ dataflow.getTasps_id() + ". SDM_DATAFLOW.dataflow_load_date="
						+ dataflow.getDataflow_load_date()+ ". Last End Run Time returned by TASPS: "+ runEndTime + "previous Last End Run Time stored into SDM: " + dataflowLastEndRunDate);
				
				continueSearch = false;
				
			} else {
				continueSearch = true;
			}
			
		}
	}
	
	//TIME_WINDOW tollerance
	public boolean checkLastRun(String endRunDate){
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Long nowTimestamp = timestamp.getTime();
		Long endRunTimestamp = Long.parseLong(endRunDate);
		log.debug("nowTimestamp: "+nowTimestamp+". endRunTimestamp: "+endRunTimestamp);
		if (LoadProperties.getProperty("THRESHOLD_LAST_RUN").equals("false")){
			return true;
		} else if (nowTimestamp-endRunTimestamp<Long.parseLong(LoadProperties.getProperty("THRESHOLD_LAST_RUN"))){
			return true;
		} else {
			return false;
		}
	}
	
	//CHECK PREVIOUS ENDRUNDATE
	public boolean checkDataflowLastRun(DataflowBean dataflow, String endRunDate){
		String dataflowLastEndRunDate= dataflow.getLast_end_run_date()==null? "" : dataflow.getLast_end_run_date();
		log.debug("previousDate: "+dataflowLastEndRunDate+". newDate: "+endRunDate);
		if (dataflowLastEndRunDate.equals("") || !endRunDate.equals("0") || !dataflowLastEndRunDate.equals(endRunDate)){
			return true;
		} else {
			return false;
		}
	}
	public void checkResponse (JsonObject response, DataflowBean dataflow){
		if (response.get("result").equals("401")){
			MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), dataflow.getDataflow_id(), dataflow.getTasps_id());
			log.info("Error updating status on DB for analysis in "+dataflow.getValue_pack()+" value pack with dataflow id: "+dataflow.getDataflow_id()+" and TASPS id: "+dataflow.getTasps_id()+". SDM_DATAFLOW.dataflow_load_date="+dataflow.getDataflow_load_date()+". Error description: "+response.get("description"));
			
		}
	}

}
