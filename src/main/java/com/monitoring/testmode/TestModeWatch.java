package com.monitoring.testmode;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.monitoring.App;
import com.monitoring.utilities.CustomConstants;
import com.monitoring.utilities.DataflowBean;
import com.monitoring.utilities.LoadProperties;
import com.monitoring.utilities.MonitoringUtilities;

/**
*
 */

public class TestModeWatch {

	static Logger log = Logger.getLogger(TestModeWatch.class.getName());

	/**
	 * Constructor that call method processEvents()
	 * 
	 * @return
	 */
	public TestModeWatch() throws IOException {
		log.info("Testing Mode Enabled - Running in Testing Mode");

		try {
			processEvents();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
	}

	/**
	 * 
	 * 
	 * @throws InterruptedException
	 * 
	 */
	void processEvents() throws InterruptedException {
		boolean continueSearch = true;
		ArrayList<DataflowBean> dfArray = null;
		List<String> roles = new ArrayList<String>();
		roles = MonitoringUtilities.addRoles(roles);
		String xAccessToken = MonitoringUtilities.mkJWT(LoadProperties.getProperty(CustomConstants.USER_API_TASPS),
				roles);
		String urlRetrieveInfoTasps = "";
		CloseableHttpClient httpClient = HttpClients.createDefault();

		while (true) {

			// PING I'm an Hardworker module and I'm still working!
			HashMap<String, Object> bodyRequestPing = new HashMap<String, Object>();
			bodyRequestPing.put("module", "MonitorModule");
			MonitoringUtilities.sendPost(httpClient,new Gson().toJson(bodyRequestPing),
					LoadProperties.getProperty("API_UPDATE_STATUS_TABLE"));

			try {
				dfArray = new ArrayList<DataflowBean>(Arrays.asList(
						MonitoringUtilities.retrieveAllDataFlow(new URL(LoadProperties.getProperty("API_GET_ALL")))));
				log.debug("Check if there are other dataflows in waiting status - API_GET_ALL called");
				// tried to bring inside for loop
				for (int i = 0; i < dfArray.size(); i++) {
					if (dfArray.get(i).getTasps_id() != null && dfArray.get(i).getTasps_id() != "") {
						urlRetrieveInfoTasps = LoadProperties.getProperty("API_RETRIEVE_STATUS_TASPS")
								+ dfArray.get(i).getTasps_id();
					} else {
						continue;
					}
					// speadmin,speoper,spedesigner,speexpertdesigner,spehpdeveloper

					CloseableHttpResponse response = MonitoringUtilities.sendGetWithoutClose(httpClient,urlRetrieveInfoTasps, xAccessToken);
					String responseBody = "";
					try {
						responseBody = EntityUtils.toString(response.getEntity());
					} catch (ParseException e1) {
						log.error(e1);
					} catch (IOException e1) {
						log.error(e1);
					} catch (Exception e) {
						log.error(e);
					} finally {
						response.close();
						httpClient.close();
					}

					JsonParser jsonParser = new JsonParser();
					JsonObject jo = (JsonObject) jsonParser.parse(responseBody);
					String resultDescription = jo.get("rows").getAsJsonArray().get(0).getAsJsonObject().get("result")
							.toString();
					String status = jo.get("rows").getAsJsonArray().get(0).getAsJsonObject().get("status").toString();
					String last_end_run_date = jo.get("rows").getAsJsonArray().get(0).getAsJsonObject()
							.get("runEndTime").toString();

					// add check on completed date TODO from TASPS response,
					// compare to actual last completed date
					if (Integer.parseInt(status) == CustomConstants.STATUS_TASPS_COMPLETE
							&& (dfArray.get(i).getLast_end_run_date() == null
									|| !dfArray.get(i).getLast_end_run_date().equals(last_end_run_date))) {
						MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_COMPLETED),
								dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
						log.info("Dataflow status complete from tasps for id " + dfArray.get(i).getTasps_id());

						// set status ready on db using tasps_id
						String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
						HashMap<String, Object> bodyRequest = TestModeWatch.createBodyRequest(
								dfArray.get(i).getTasps_id(), CustomConstants.STATUS_COMPLETE, resultDescription,
								last_end_run_date);

						MonitoringUtilities.updateStatus(urlString, bodyRequest);

						MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_COMPLETED),
								"", "");
						log.info(
								"Dataflow status complete successfully updated on db for id " + dfArray.get(i).getId());
						continueSearch = false;
					} else if (Integer.parseInt(status) == CustomConstants.STATUS_TASPS_ERROR
							&& (dfArray.get(i).getLast_end_run_date() == null
									|| !dfArray.get(i).getLast_end_run_date().equals(last_end_run_date))) {
						MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.DATAFLOW_ERROR),
								dfArray.get(i).getDataflow_id(), dfArray.get(i).getTasps_id());
						log.info("Dataflow status error from tasps for id " + dfArray.get(i).getTasps_id());
						String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
						HashMap<String, Object> bodyRequest = TestModeWatch.createBodyRequest(
								dfArray.get(i).getTasps_id(), CustomConstants.STATUS_ERROR, resultDescription,
								last_end_run_date);
						MonitoringUtilities.updateStatus(urlString, bodyRequest);

						MonitoringUtilities.setLogInfo("LOG", "", "");
						log.info("Dataflow status error successfully updated on db for id " + dfArray.get(i).getId());
					} else {
						continueSearch = true;
					}

				}
			} catch (MalformedURLException e1) {
				log.error(e1);
			} catch (Exception e) {
				log.error(e);
			}

			try {
				App.getTaskExecutor().setKeepAliveSeconds(120);
				Thread.sleep(Integer.parseInt(LoadProperties.getProperty("THREAD_POLLING_WAIT")));
			} catch (InterruptedException e) {
				log.error(e);
			} catch (Exception e) {
				log.error(e);
			}
		}

	}

	private static HashMap<String, Object> createBodyRequest(String tasps_id, int status, String description_status,
			String last_end_run_date) {
		HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
		bodyRequest.put("tasps_id", tasps_id);
		bodyRequest.put("status", status);
		bodyRequest.put("description_status", description_status);
		bodyRequest.put("last_end_run_date", last_end_run_date);
		return bodyRequest;
	}

}
