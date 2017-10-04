package com.monitoring.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class LoadProperties {

	static Logger log = Logger.getLogger(LoadProperties.class.getName());

	private static Properties properties = null;
	private static long lastModified = 0;
	private static String PROPERTIES = CustomConstants.SOURCE_DIR_LOCAL + CustomConstants.PROPERTIES_FLE;

	private static void initialize() {

		try {
			File fileConfig = new File(PROPERTIES);
			if (lastModified < fileConfig.lastModified()) {
				log.info("Loaded properties from: " + PROPERTIES);
				properties = null;
				properties = new Properties();
				properties.load(new FileInputStream(fileConfig));
				lastModified = fileConfig.lastModified();
			}
		} catch (Exception e) {
			log.error(e);
		}

	}

	public static String getProperty(String key) {
		initialize();
		String prop;
		try {
			prop = properties.getProperty(key);
		} catch (NullPointerException e) {
			PROPERTIES = CustomConstants.SOURCE_DIR_LOCAL + CustomConstants.PROPERTIES_FLE;
			initialize();
			prop = properties.getProperty(key);
		}
		return prop;
	}

	public static Properties getProperties() {
		initialize();
		return properties;
	}

	public static long getLastModified() {
		return lastModified;
	}

	public static void setLastModified(long lastModified) {
		LoadProperties.lastModified = lastModified;
	}

}
