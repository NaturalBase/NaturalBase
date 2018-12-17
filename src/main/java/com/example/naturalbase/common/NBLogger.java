package com.example.naturalbase.common;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NBLogger {

	private static Logger logger;
	
	private NBLogger() {
		if (logger == null) {
			logger = LoggerFactory.getLogger(this.getClass());
		}
	}
	
	public static Logger Inst() {
		if (logger == null) {
			logger = LoggerFactory.getLogger(NBLogger.class);
		}
		return logger;
	}
	
	public static void debug(String d) {
		logger = LoggerFactory.getLogger(NBLogger.class);
		logger.debug(d);
	}
	
	public static void info(String d) {
		logger = LoggerFactory.getLogger(NBLogger.class);;
		logger.info(d);
	}
	
	public static void warn(String d) {
		logger = LoggerFactory.getLogger(NBLogger.class);;
		logger.warn(d);
	}
	
	public static void error(String d) {
		logger = LoggerFactory.getLogger(NBLogger.class);;
		logger.error(d);
	}
	
	public static void debug(String d, Class<?> clazz) {
		logger = LoggerFactory.getLogger(clazz);
		logger.debug(d);
	}
	
	public static void info(String d, Class<?> clazz) {
		logger = LoggerFactory.getLogger(clazz);;
		logger.info(d);
	}
	
	public static void warn(String d, Class<?> clazz) {
		logger = LoggerFactory.getLogger(clazz);;
		logger.warn(d);
	}
	
	public static void error(String d, Class<?> clazz) {
		logger = LoggerFactory.getLogger(clazz);;
		logger.error(d);
	}
}
