package com.monitoring.folder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.monitoring.utilities.CustomConstants;
import com.monitoring.utilities.DataflowBean;
import com.monitoring.utilities.LoadProperties;
import com.monitoring.utilities.MonitoringUtilities;
import com.monitoring.utilities.QueuesManager;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class DirectoryWatch {

	static Logger log = Logger.getLogger(DirectoryWatch.class.getName());
	private DataflowBean dataflow;
	private List<String> regexRules;
	
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	

	private Queue<String> queueFile = new LinkedList<String>();

	public Queue<String> getQueueFile() {
		return queueFile;
	}

	public void setQueueFile(Queue<String> queueFile) {
		this.queueFile = queueFile;
	}

	@SuppressWarnings("unchecked")


	/**
	 * Creates a WatchService and registers the given directory
	 * 
	 * @return
	 */
	public DirectoryWatch(DataflowBean dataflow, String recursive) throws IOException {

		String dirpath = dataflow.getFolder();
		 log.debug("Inside DirectoryWatch Constructor");

		this.dataflow = dataflow;
		regexRules = new ArrayList<String>(Arrays.asList(dataflow.getRegex().split(";")));
		try {
			processEvents();
		}  catch (Throwable e) {
		    log.error("Uncaught exception", e); 
		}
	}

	/**
	 * Process all events for keys queued to the watcher
	 * 
	 * @throws FileNotFoundException
	 */
	void processEvents() throws FileNotFoundException {
		// while dataflow isn't sent to queue ready, continueSearch
		boolean continueSearch = true;
		
		while (continueSearch) {
			// take dataflow load date variable
			
			String dataflow_load_date = dataflow.getDataflow_load_date();

			log.trace("Before check if file already exist. Continue to search? " + continueSearch);
			String dirpath = dataflow.getFolder();
			// log.info("Inside DirectoryWatch Constructor");

			Path dir = Paths.get(dirpath);
			
			// CHECK IF FILES ARE ALREADY PRESENT IN FOLDER ON FIRST RUN FOR HOURLY 
			if(checkIfHourlyDataFlow(dataflow)) {
				log.info("Inside the Hourly Data flow ...");
				Calendar dataFlowLoadDate =  Calendar.getInstance();
				try {
					dataFlowLoadDate.setTime(simpleDateFormat.parse(dataflow_load_date));
					dataFlowLoadDate.add(Calendar.DATE, 1);
					
				} catch (ParseException e) {
					e.printStackTrace();
				}

				String nextDayDataFlowLoadDate = simpleDateFormat.format(dataFlowLoadDate.getTime());
				log.info("Checking for next date file in the directory, dataflow+1   :" +nextDayDataFlowLoadDate );
				
				if(checkFilesAlreadyExistForNextDay(nextDayDataFlowLoadDate,dir)) {
					log.info("inside the checkFilesAlreadyExist method for checking for next date finles in the dir");
					if(dataflow.getStatus() !=CustomConstants.STATUS_COMPLETE) 
					updateHourlyDataFlowStatusAsComplete(dataflow);
					break;
					
				} else {
					continueSearch = this.checkFilesAlreadyExist(dataflow_load_date, dir);
				}
			}
			
			// CHECK IF FILES ARE ALREADY PRESENT IN FOLDER ON FIRST RUN.
			continueSearch = this.checkFilesAlreadyExist(dataflow_load_date, dir);
			log.trace("Result after check if file already exist. Continue to search? " + continueSearch);

			try {
				Thread.sleep(Integer.parseInt(LoadProperties.getProperty("THREAD_POLLING_WAIT")));
			} catch (InterruptedException e) {
				log.debug("close interuupted monitor watcher folder");
				continueSearch=false;
				break;
			}
			
			// reset key and remove from set if directory no longer accessible
	
		}
	}

	private void updateHourlyDataFlowStatusAsComplete(DataflowBean dataflow) {
		String urlString = LoadProperties.getProperty("API_UPDATE_STATUS_BY_TASPS_ID");
		HashMap<String, Object> bodyRequest = new HashMap<String, Object>();
		bodyRequest.put("tasps_id", dataflow.getTasps_id());
		bodyRequest.put("status", CustomConstants.STATUS_COMPLETE);
		JsonObject responseError = MonitoringUtilities.updateStatus(urlString, bodyRequest);
		log.info(" Inside the updateHourlyDataFlowStatusAsComplete ::if next date file is avialbel in the target dir then set the status complete tasps_id:"+dataflow.getTasps_id() +" and status:"+CustomConstants.STATUS_COMPLETE);
	}

	private boolean checkIfHourlyDataFlow(DataflowBean dataflow) {
		if(dataflow.getFrequency() == CustomConstants.FREQUENCY_HOURLY) {
			log.info("checking if it is hourly Dataflow and  frequency is :" + dataflow.getFrequency());
			return true;
		}
		log.info("checking if it is not  hourly Dataflow and  frequency is :" + dataflow.getFrequency());
		return false;
	}

	
	/*
	 * new change in code  Checking for next date file in the dir for the hourly dataflow
	 */
	
	private boolean checkFilesAlreadyExistForNextDay(String dataflow_load_date, Path dir) {

		log.info("Inside the checkFilesAlreadyExistForNextDay method for checking the next day file for hourly dataflow ");
		String dirName = dir.toString();
		boolean bName=false;
		File folderDirectory = new File(dirName);
		File[] dir_contents = folderDirectory.listFiles();
		boolean continueSearch = true;
		log.trace(dir_contents.length);
		for (int j = 0; j < dir_contents.length; j++) {
			log.trace(dir_contents[j].getAbsoluteFile());
			for (int i = 0; i < regexRules.size(); i++) {
				String currentRegex = regexRules.get(i);
				// logic date year month day
				
				if (currentRegex.contains("YY") && currentRegex.contains("MM") && currentRegex.contains("DD")) {
					currentRegex = MonitoringUtilities.replaceRegexDate(dataflow_load_date, currentRegex);
					log.trace("Current Regex: " + currentRegex);
				}
				Pattern uName = Pattern.compile(currentRegex);
				Matcher mUname = uName.matcher(dir_contents[j].getName().toString());
				bName = mUname.matches();
				log.info("returning the  nextFileDay  status for hourly dataflow: "+ bName);
				if (bName)
					return true;
			}
		}
		return  bName;
	}
	
	
	private boolean checkFilesAlreadyExist(String dataflow_load_date, Path dir) {

		String dirName = dir.toString();
		File folderDirectory = new File(dirName);
		File[] dir_contents = folderDirectory.listFiles();
		boolean continueSearch = true;
		log.trace(dir_contents.length);
		for (int j = 0; j < dir_contents.length; j++) {
			log.trace(dir_contents[j].getAbsoluteFile());
			for (int i = 0; i < regexRules.size(); i++) {
				String currentRegex = regexRules.get(i);
				// logic date year month day
				
				if (currentRegex.contains("YY") && currentRegex.contains("MM") && currentRegex.contains("DD")) {
					currentRegex = MonitoringUtilities.replaceRegexDate(dataflow_load_date, currentRegex);
					log.trace("Current Regex: " + currentRegex);
				}
				Pattern uName = Pattern.compile(currentRegex);
				Matcher mUname = uName.matcher(dir_contents[j].getName().toString());
				boolean bName = mUname.matches();
				if (bName) {
					log.trace("match checkFilesAlreadyExist, before fileFound() " + dir_contents[j]);
					this.fileFound(currentRegex, dir, i);
					if (queueFile.size() == this.dataflow.getNumber_files()) {
						this.allFileFound();
						continueSearch = false;
						return continueSearch; // stop search
					}
					break;
				}

			}
		}
		return continueSearch;
	}

	private void allFileFound() {

		// send alert to Queue Trigger
		MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), dataflow.getDataflow_id(), dataflow.getTasps_id());
		log.info("For analysis in "+ dataflow.getValue_pack()+ " value pack with dataflow id: "+dataflow.getDataflow_id()+ " and TASPS id: "+dataflow.getTasps_id()+" all files needed for the analysis have been retrieved. Trigger Task will start the analysis on TASPS. SDM_DATAFLOW.dataflow_load_date="+dataflow.getDataflow_load_date());
	 //  ADD DELAY after ALL FILE FOUND
		try {
			log.info("For analysis in "+ dataflow.getValue_pack()+ " value pack with dataflow id: "+dataflow.getDataflow_id()+ " and TASPS id: "+dataflow.getTasps_id()+". Wait default time before to trigger this job.");
			Thread.sleep(Long.parseLong(LoadProperties.getProperty("WAIT_AFTER_ALL_FILES")));
			//IS IT CORRECT call there? RetrieveInfoWatch.getPendingDataflow()
			//RetrieveInfoWatch.getPendingDataflow().remove(dataflow.getId());
			QueuesManager.getQueueFileAvailable().add(dataflow);
		} catch (NumberFormatException e) {
			log.error("Wrong Properties WAIT_AFTER_ALL_FILES");
		} catch (InterruptedException e) {
			log.info("Interrupted Thread sleep after all file founds for "+dataflow.getDataflow_id());
		}
		
	}

	private void fileFound(String currentRegex, Path child, int i) {
		MonitoringUtilities.setLogInfo(LoadProperties.getProperty(CustomConstants.LOG), dataflow.getDataflow_id(), dataflow.getTasps_id());
		log.info("For analysis in "+ dataflow.getValue_pack()+ " value pack with dataflow id: "+dataflow.getDataflow_id()+ " and TASPS id: "+dataflow.getTasps_id()+" new file has been created: " + child + " that matches with " + currentRegex +". SDM_DATAFLOW.dataflow_load_date="+dataflow.getDataflow_load_date());

		// add file name to dataflow queue and after check length
		queueFile.add(child.getFileName().toString());
		regexRules.remove(i);
		log.debug("Find dataflow file dependencies, regex rule found removed from regex rules list for dataflow "
				+ dataflow.getId());
	}

}
