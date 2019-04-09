/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2019. All rights reserverd.
 *
 */

package com.example.naturalbase.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Enumeration;
import org.apache.commons.io.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StructureLog is a public class to record the structure log and trans it to AIOps by flume agent.
 *
 * @author zhouxinman[zhouxinman@huawei.com]
 * @since 2019-03-22
 */

public class StructureLog {
	private static ConcurrentHashMap<String, String> fieldConMap;
	private static ArrayList<String> fieldList;
	private static File logFile;
	private static final String STRUCTURELOG_JSON_PATH = "/opt/huawei/apps/hispace/mBaasSyncServer/cfg/structureLog.json";
	private static final String STRUCTURELOG_FILE_PATH = "/opt/huawei/logs/hispace/mBaasSyncServer/naturalbase.log";
	private static final int STRUCTURELOG_WRITEFILE_TIMERVALUE = 30000;
	private static final Logger logger = LoggerFactory.getLogger(StructureLog.class);
	
	public StructureLog() {
		fieldConMap = new ConcurrentHashMap<String, String>();
		fieldList = new ArrayList<String>();
		parseStrucctureLogFile();
		structLogGetHost();
		structLogCreatTimerTask();
	}
	
	private void parseStrucctureLogFile() {
		String jsonFileContent = "";
		
		try {
			logFile = new File(STRUCTURELOG_FILE_PATH);
			if(!logFile.exists()) {
	            logFile.createNewFile();
			}
		    File jsonFile = new File(STRUCTURELOG_JSON_PATH);
	        jsonFileContent = FileUtils.readFileToString(jsonFile, "UTF-8");
		} catch (IOException ex) {
			logger.error("Structurelog exception. Cause:" + ex.getCause().toString());
			return;
		} 
		
		
		try {
			JSONObject jsonContent = JSONObject.parseObject(jsonFileContent);
			if (jsonContent == null) {
				logger.error("Structurelog jsonContent is null");
				return;
			}
			JSONArray fieldInfo = jsonContent.getJSONArray("fieldInfo");
			int fieldCount = fieldInfo.size();
			logger.debug("Structurelog fieldCount =" + fieldCount);
			for (int i = 0; i < fieldCount; i++) {
				JSONObject jsonSubContent = fieldInfo.getJSONObject(i);
				JSONArray keyInfo = jsonSubContent.getJSONArray("key");
				JSONArray valueInfo = jsonSubContent.getJSONArray("value");
				logger.debug("Structurelog keyInfo =" + keyInfo.getString(0));
				logger.debug("Structurelog valueInfo =" + valueInfo.getString(0));
				fieldList.add(keyInfo.getString(0));
				concurrentHashMapAddOrSetFiled(keyInfo.getString(0), valueInfo.getString(0));
			}
		} catch (JSONException ex) {
			logger.error("Structurelog exception. Cause:" + ex.getCause().toString());
			return;
		}
	}
	
	private void structLogWriteFile() {
		String logStr = "";
		FileWriter fileWritter = null;
		
		for (String name:fieldList) {
			if (logStr != "") {
				logStr += "|";
			}
			logger.debug("Structurelog name =" + name);
			logStr += fieldConMap.get(name);
		}

		logStr += System.lineSeparator();
		logger.debug("Structurelog logStr =" + logStr);
		
		try {
			fileWritter = new FileWriter(logFile.getPath(),true);
		    fileWritter.write(logStr);
		    fileWritter.flush();

		} catch (IOException ex) {
			logger.error("Structurelog exception. Cause:" + ex.getCause().toString());
			return;
		} finally {
			try {
			    fileWritter.close();
			} catch  (IOException ex) {
				logger.error("Structurelog exception. Cause:" + ex.getCause().toString());
				return;
			}
		}
	}
	
	private void structLogGetHost() {
		concurrentHashMapAddOrSetFiled("hostIP", getInet4Address());
		concurrentHashMapAddOrSetFiled("hostName", getHostName());
	}
	
	private void structLogCreatTimerTask() {
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				structLogWriteFile();
			}
		}, STRUCTURELOG_WRITEFILE_TIMERVALUE, STRUCTURELOG_WRITEFILE_TIMERVALUE);
	}
	
	public void concurrentHashMapAddOrSetFiled(String fieldName, String fieldValue) {
		if (fieldName == null || fieldValue == null) {
			return;
		}
		
		if (fieldConMap == null) {
			logger.error("Structurelog fieldConMap is null");
			return;
		}
		fieldConMap.put(fieldName, fieldValue);
	}
	
	public String getInet4Address() {
	    Enumeration<NetworkInterface> eNetIf;
		String ip = "";
		try {
			eNetIf = NetworkInterface.getNetworkInterfaces();
			for (; eNetIf.hasMoreElements();) {
				NetworkInterface netIf = eNetIf.nextElement();
				Enumeration<InetAddress> eInetAddr = netIf.getInetAddresses();
				for (; eInetAddr.hasMoreElements();) {
					InetAddress inetAddr = eInetAddr.nextElement();
					if (inetAddr instanceof Inet4Address && !inetAddr.getHostAddress().contains("127.0.0.")) {						
						ip = inetAddr.getHostAddress();
					}
				}
			}
		} catch (SocketException ex) {
			logger.error("Structurelog exception. Cause:" + ex.getCause().toString());
		}
		logger.debug("Structurelog ip =" + ip);
		return ip;
	}
	
	public String getHostName() {  
	    try {  
	        return (InetAddress.getLocalHost()).getHostName();  
	    } catch (UnknownHostException uhe) {  
	        String host = uhe.getMessage();
	        if (host != null) {  
	            int colon = host.indexOf(':');  
	            if (colon > 0) {  
	                return host.substring(0, colon);  
	            }  
	        }  
	        return "UnknownHost";  
	    }  
	} 
	
	public void structureLogTest() {
		concurrentHashMapAddOrSetFiled("naturalBaseCount", "1");
		concurrentHashMapAddOrSetFiled("naturalStoreCount", "1");
		concurrentHashMapAddOrSetFiled("connectCount", "10");
		concurrentHashMapAddOrSetFiled("syncDataPerMin", "10");
		concurrentHashMapAddOrSetFiled("syncDataPerHour", "10");
		concurrentHashMapAddOrSetFiled("httpErrorCount", "2");
		concurrentHashMapAddOrSetFiled("tcpErrorCount", "2");
		concurrentHashMapAddOrSetFiled("dbErrorCount", "2");
		concurrentHashMapAddOrSetFiled("dbErrorHostIP", "10.31.29.165");
	}
	
}