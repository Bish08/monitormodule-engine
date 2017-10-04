package com.monitoring.utilities;

public class CustomConstants {

	//public static final String SOURCE_DIR_LOCAL = "./src/main/resources/";
	//public static final String SOURCE_DIR_LOCAL = "/opt/monitoring/";
	public static final String SOURCE_DIR_LOCAL = "/home/sps/monitoring/";

	public static final String PROPERTIES_FLE ="app.properties";
	
	public static final int STATUS_WAITING =0;
	public static final int STATUS_MONITORING =1;
	public static final int STATUS_INPROGRESS =2;
	public static final int STATUS_COMPLETE =3;
	public static final int STATUS_ERROR =4;
	public static final int STATUS_DELETE =5;
	public static final int STATUS_SCHEDULED =6;
	public static final int STATUS_HOURLY_COMPLETE =7;
	public static final String SECRET ="SECRET_TASPS";

	public static final int STATUS_TASPS_COMPLETE =4;
	public static final int STATUS_TASPS_ERROR =5;
	public static final int STATUS_TASPS_ABORTED =6;

	public static final String DB_DATE_FORMAT =	"yyyy-MM-dd HH:mm:ss";
	public static final String DB_DATEONLY_FORMAT =	"yyyy-MM-dd";
	public static final String DB_MONTH_FORMAT =	"yyyy-MM";
	public static final String DB_HOURLY_FORMAT =	"yyyy.MM.dd.HH";
	
	public static final int FREQUENCY_ONCE = 0;
	public static final int FREQUENCY_MULTIPLE = 1;
	public static final int FREQUENCY_MONTHLY = 2;
	public static final int FREQUENCY_HOURLY = 3;
	
	public static final String DATAFLOW_MANUALLY_TRIGGERED="DATAFLOW_MANUALLY_TRIGGERED"; 
	public static final String DATAFLOW_FILES_AV_TRIGGERED="DATAFLOW_FILES_AV_TRIGGERED";
	public static final String DATAFLOW_CHECK_DEPENDECIES_TRIGGERED="DATAFLOW_CHECK_DEPENDECIES_TRIGGERED";
	public static final String DATAFLOW_COMPLETED="DATAFLOW_COMPLETED";
	public static final String DATAFLOW_ERROR="DATAFLOW_ERROR";
	public static final String INCREMENT_LOAD_DATE="INCREMENT_LOAD_DATE";
	public static final String EDIT_LOAD_DATE="EDIT_LOAD_DATE";
	public static final String STOP_MONITORING="STOP_MONITORING";
	public static final String START_MONITORING="START_MONITORING";
	
	public static final String LOG="LOG";
	public static final String EDIT_DATAFLOW="DATAFLOW_EDIT";
	public static final String NO_DATAFLOW="NO_DATAFLOW";
	
	public static final String USER_API_TASPS="USER_API_TASPS";
	public static final String API_PUT_TASPS_STATUS = "api_put_tasps_status";
}
