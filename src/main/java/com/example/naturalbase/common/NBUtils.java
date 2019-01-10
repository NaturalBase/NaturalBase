package com.example.naturalbase.common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

public class NBUtils {
	
	private static Logger logger = LoggerFactory.getLogger(NBUtils.class);
	
	public static String  generateErrorInfo(String s) {
		JSONObject responseBody = new JSONObject();
		responseBody.put("Reason", s);
		
		String retStr = new String();
		try {
			retStr = new String(responseBody.toJSONString().getBytes(), "UTF-8");
		}
		catch (Exception e) {
			logger.error("NBUtils generateErrorInfo encode catch exception. cause:" + e.getCause());
		}
		
		return retStr;
	}
	
	public static String ToUTF8String(byte[] b) {
		String retStr = new String();
		try {
			retStr = new String(b, "UTF-8");
		}
		catch (Exception e) {
			logger.error("NBUtils ToUTF8String encode catch exception. cause:" + e.getCause());
		}
		return retStr;
	}
	
	public static String ByteArrayToHexString(byte[] b) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<b.length; i++) {
			builder.append(String.format("0x%x ", b[i]));
		}
		
		return builder.toString();
	}
	
	public static long GetCurrentTimeStamp() {
		long currentTimeMs = System.currentTimeMillis();
		long currentNanoTime = System.nanoTime();
		final int K = 1000;
		final int M = K * K;
		long currentTimeUs = 0;
		
		currentTimeUs = currentTimeMs * K;
		currentTimeUs = currentTimeUs + (currentNanoTime-(currentNanoTime/M)*M)/K;
		return currentTimeUs;
	}
}
