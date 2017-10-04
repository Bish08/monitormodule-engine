package com.monitoring.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.auth0.jwt.JWTSigner;
import com.auth0.jwt.JWTSigner.Options;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MonitoringUtilities {
	static Logger log = Logger.getLogger(MonitoringUtilities.class.getName());

	public static DataflowBean retrieveDataFlow(URL url) {
		String xAccessToken = "";
		CloseableHttpClient httpClient = HttpClients.createDefault();

		CloseableHttpResponse response = MonitoringUtilities.sendGet(httpClient, url.toString(), xAccessToken);
		String responseBody = "";
		try {
			responseBody = EntityUtils.toString(response.getEntity());
		} catch (ParseException e1) {

			log.error("Exception", e1);
		} catch (IOException e1) {

			log.error("Exception", e1);
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
		ObjectMapper mapper = new ObjectMapper();
		// JSON from URL to Object
		if (jo.get("result") == null) {
			try {
				return mapper.readValue(url, DataflowBean.class);
			} catch (JsonParseException e1) {

				log.error("Exception", e1);
				return null;
			} catch (JsonMappingException e1) {

				log.error("Exception", e1);
				return null;
			} catch (MalformedURLException e1) {

				log.error("Exception", e1);
				return null;
			} catch (IOException e1) {

				e1.printStackTrace();
				return null;

			}
		} else {
			return null;
		}

	}

	public static DataflowBean[] retrieveAllDataFlow(URL url) {
		ObjectMapper mapper = new ObjectMapper();
		// JSON from URL to Object
		try {
			return mapper.readValue(url, DataflowBean[].class);
		} catch (JsonParseException e1) {

			log.error("Exception", e1);
			return null;
		} catch (JsonMappingException e1) {

			log.error("Exception", e1);
			return null;
		} catch (MalformedURLException e1) {

			log.error("Exception", e1);
			return null;
		} catch (IOException e1) {

			log.error("Exception", e1);
			return null;

		}

	}

	public static JsonObject updateStatus(String urlString, HashMap<String, Object> bodyRequest) {
		JsonObject responseTasps = MonitoringUtilities.sendPut(new Gson().toJson(bodyRequest), urlString,null);
		return responseTasps;
	}

	public static JsonObject sendPut(String data, String url, String xAccessToken) {
		int responseCode = -1;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		CloseableHttpResponse response = null;
		HttpPut request = new HttpPut(url);
		JsonObject jo = new JsonObject();
		String responseBody = "";
		StringEntity params = new StringEntity(data, "UTF-8");
		params.setContentType("application/json");
		request.addHeader("content-type", "application/json");
		if (xAccessToken != null) {
			request.addHeader("x-access-token", xAccessToken);
		}
		
		request.setEntity(params);
		try {

			response = httpClient.execute(request);
			// responseCode = response.getStatusLine().getStatusCode();
			responseBody = EntityUtils.toString(response.getEntity());
			JsonParser jsonParser = new JsonParser();
			jo = (JsonObject) jsonParser.parse(responseBody);
		} catch (Exception ex) {
			log.error("Exception", ex);
		} finally {
			try {
				response.close();
				httpClient.close();
			} catch (IOException e) {

				log.error("ERROR", e);
			}
		}

		return jo;

	}

	public static CloseableHttpResponse sendPost(CloseableHttpClient httpClient, String data, String url) {
		int responseCode = -1;
		CloseableHttpResponse response = null;
		HttpPost request = new HttpPost(url);
		StringEntity params = new StringEntity(data, "UTF-8");
		params.setContentType("application/json");
		request.addHeader("content-type", "application/json");
		request.setEntity(params);
		try {
			response = httpClient.execute(request);

			// responseCode = response.getStatusLine().getStatusCode();

		} catch (Exception ex) {
			log.error("Exception", ex);
		}

		return response;

	}

	public static CloseableHttpResponse sendGet(CloseableHttpClient httpClient, String url, String xAccessToken) {
		CloseableHttpResponse response = null;
		try {
			HttpGet request = new HttpGet(url);
			request.addHeader("content-type", "application/json");
			request.addHeader("x-access-token", xAccessToken);
			response = httpClient.execute(request);

		} catch (Exception ex) {
			log.error("Exception", ex);
		} finally {
			try {
				response.close();
				httpClient.close();
			} catch (IOException e) {

				log.error("ERROR", e);
			}
		}
		return response;

	}

	public static CloseableHttpResponse sendGetWithoutClose(CloseableHttpClient httpClient, String url,
			String xAccessToken) {
		CloseableHttpResponse response = null;
		try {
			HttpGet request = new HttpGet(url);
			request.addHeader("content-type", "application/json");
			if (xAccessToken != null) {
				request.addHeader("x-access-token", xAccessToken);
			}
			response = httpClient.execute(request);

		} catch (Exception ex) {
			log.error("Exception", ex);
		}
		return response;

	}

	public static String mkJWT(String username, List<String> roles) {
		String secret = LoadProperties.getProperty(CustomConstants.SECRET);
		JWTSigner signer = new JWTSigner(secret);
		Map<String, Object> claims = new HashMap<String, Object>();
		claims.put("id", username);
		claims.put("roles", roles);
		Options options = new Options();
		// can play with a few of those options
		return signer.sign(claims, options);
	}

	public static List<String> addRoles(List<String> roles) {
		roles.add("speadmin");
		roles.add("speoper");
		roles.add("spedesigner");
		roles.add("speexpertdesigner");
		roles.add("spehpdeveloper");
		return roles;
	}

	public static void setLogInfo(String event_type, String dataflow_id, String tasps_id) {
		MDC.put("EVENT_TYPE", event_type);
		MDC.put("DATAFLOW_ID", dataflow_id);
		MDC.put("TASPS_ID", tasps_id);
	}

	public static String convertStreamtoString(InputStream is) {

		String line = "";
		String data = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {

				data += line;
			}
		} catch (Exception e) {
			log.error("Exception", e);
		}
		return data;
	}

	public static String replaceRegexDate(String dataflow_load_date, String currentRegex) {
		DateFormat dateFormat = new SimpleDateFormat(CustomConstants.DB_DATEONLY_FORMAT);
		String load_date_year = null;
		String load_date_month = null;
		String load_date_day = null;
		Date date_load;
		try {
			date_load = dateFormat.parse(dataflow_load_date);
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date_load);
			load_date_year = Integer.toString(calendar.get(Calendar.YEAR));
			load_date_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
			load_date_day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
			load_date_month = Integer.parseInt(load_date_month) < 10 ? ("0" + load_date_month) : (load_date_month);
			load_date_day = Integer.parseInt(load_date_day) < 10 ? ("0" + load_date_day) : (load_date_day);
		} catch (java.text.ParseException e1) {

			e1.printStackTrace();
		}
		currentRegex = currentRegex.replace("YYYY", load_date_year);
		currentRegex = currentRegex.replace("YY", load_date_year.substring(2));
		currentRegex = currentRegex.replace("MM", load_date_month);
		currentRegex = currentRegex.replace("DD", load_date_day);
		currentRegex = currentRegex.replace("hhmmss", "*");
		currentRegex = currentRegex.replace("fff", "");
		return currentRegex;
	}

}