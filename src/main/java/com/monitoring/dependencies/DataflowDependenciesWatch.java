package com.monitoring.dependencies;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.monitoring.utilities.CustomConstants;
import com.monitoring.utilities.DataflowBean;
import com.monitoring.utilities.LoadProperties;
import com.monitoring.utilities.MonitoringUtilities;
import com.monitoring.utilities.QueuesManager;

/**
*
 */

public class DataflowDependenciesWatch {

	static Logger log = Logger.getLogger(DataflowDependenciesWatch.class.getName());

	private DataflowBean dataflow;
	private List<String> dataflowIds;

	/**
	 * Constructor that call method processEvents()
	 * 
	 * @return
	 */
	public DataflowDependenciesWatch(DataflowBean dataflow) throws IOException {

		log.debug("Inside DataflowDependenciesWatch Constructor");

		this.dataflow = dataflow;
		dataflowIds = new ArrayList<String>(Arrays.asList(dataflow.getDataflow_dependencies().split(";")));
		log.debug(dataflow.getDataflow_id()+"dataflow dependency watch.."+ dataflowIds);
		try {
			processEvents();
		}  catch (Throwable e) {
		    log.error("Uncaught exception", e); 
		}
	}

	/**
	 * 
	 * Check if the dataflow dependencies are completed
	 * 
	 * @throws FileNotFoundException
	 */
	void processEvents() {
		boolean continueSearch = true;
		String responseBody = "";
		JsonArray listOfDataflow = null;

		//retrieve Load Date from Value Pack - FIX 17 May 2017
		String dataflow_load_date = valuePackLoadDate(dataflow);
		// while dataflow isn't sent to queue ready, continueSearch
		while (continueSearch) {
			// TODO remove TASPS dependencies that doesn't exist - Nice To have
			for (int i = 0; i < dataflowIds.size(); i++) {
				if (dataflowIds.get(i) == "") {
					log.debug("Removed empty dependencies " + dataflowIds.get(i));
					dataflowIds.remove(i);
					continue;
				}
				CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse httpResponse = null;
				HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
				bodyRequest.put("tasps_id", dataflowIds.get(i));
				bodyRequest.put("dataflow_load_date", dataflow_load_date);
				httpResponse = MonitoringUtilities.sendPost(httpClient, new Gson().toJson(bodyRequest),
						LoadProperties.getProperty("API_GET_BY_TASPS_AND_STATUS"));
						log.info("Dataflow_load_date :"+ dataflow_load_date+" and tasps_id: "+dataflowIds.get(i));
						
						
				try {
					responseBody = EntityUtils.toString(httpResponse.getEntity());
				} catch (ParseException e1) {
					log.error(e1);
					log.info("Dataflow_load_date :"+ dataflow_load_date+" and tasps_id: "+dataflowIds.get(i));
					
				} catch (IOException e1) {
					log.error(e1);
					log.info("Dataflow_load_date: "+ dataflow_load_date+" and tasps_id: "+dataflowIds.get(i));
				} finally {
					try {
						httpResponse.close();
						httpClient.close();
					} catch (IOException e) {
						log.error("ERROR", e);
						log.info("Dataflow_load_date: "+ dataflow_load_date+" and tasps_id: "+dataflowIds.get(i));
					}
				}
				JsonParser jsonParser = new JsonParser();
				listOfDataflow = (JsonArray) jsonParser.parse(responseBody);
				log.info("this is inside the dataflowDepndency checking all competed dependency"+responseBody);

				if (listOfDataflow.getAsJsonArray().size() != 0) {
					for (int j = 0; j < listOfDataflow.getAsJsonArray().size(); j++) {

						if (Integer.parseInt(listOfDataflow.getAsJsonArray().get(j).getAsJsonObject().get("status")
								.toString()) == CustomConstants.STATUS_COMPLETE) {
							MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), dataflow.getDataflow_id(), dataflow.getTasps_id());
							log.info("For this analysis in "+ dataflow.getValue_pack()+ " with dataflow_id "+ dataflow.getDataflow_id()+" and tasps_id "+dataflow.getTasps_id()+" has been found analysis dependencies "+ dataflowIds.get(i) + " with status complete. SDM_DATAFLOW.dataflow_load_date="+dataflow.getDataflow_load_date());
							dataflowIds.remove(i);
							break;
						}
					}
				}

			}

			log.info(dataflow.getDataflow_id());

			if (dataflowIds.size() == 0) {
				MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), dataflow.getDataflow_id(), dataflow.getTasps_id());
				log.info("For this analysis in "+ dataflow.getValue_pack()+ " with dataflow_id "+ dataflow.getDataflow_id()+" and tasps_id "+dataflow.getTasps_id()+" has been found all analysis dependencies. Trigger Task will start the analysis on TASPS. SDM_DATAFLOW.dataflow_load_date="+dataflow.getDataflow_load_date());
					QueuesManager.getQueueDataflowCompleted().add(dataflow);

				continueSearch = false;
				break;
			}
			
			try {
				Thread.sleep(Integer.parseInt(LoadProperties.getProperty("THREAD_POLLING_WAIT")));
			} catch (InterruptedException e) {
				continueSearch= false;
				log.debug("Interrupted DataflowDependencies Task", e);
			}
		}
	}
	
	/********Utilities Methods Dataflow Dependencies ********/
	private String valuePackLoadDate (DataflowBean dataflow){
		String responseBodyValuePack = "";
		CloseableHttpClient httpClientValuePack = HttpClients.createDefault();
		CloseableHttpResponse httpResponseValuePack = null;
		String url = LoadProperties.getProperty("API_GET_BY_VALUE_PACK") + dataflow.getValue_pack();
		httpResponseValuePack = MonitoringUtilities.sendGetWithoutClose(httpClientValuePack, url, null);
		try {
			responseBodyValuePack = EntityUtils.toString(httpResponseValuePack.getEntity());
		} catch (ParseException e1) {
			log.error(e1);
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
		
		String dataflow_load_date = jsonParser.parse(responseBodyValuePack).getAsJsonObject().get("dataflow_load_date").toString().replaceAll("\"", "");
		log.trace("ValuePack Dataflow_load_Date: "+dataflow_load_date);
		return dataflow_load_date;
	}
}
