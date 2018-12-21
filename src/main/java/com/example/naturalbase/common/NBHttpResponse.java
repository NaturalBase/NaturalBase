package com.example.naturalbase.common;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NBHttpResponse {
	private HttpStatus statusCode;
	private String returnStr = new String();
	private Logger logger = LoggerFactory.getLogger(NBUtils.class);
	
	public NBHttpResponse() {
		statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
		returnStr = "server error";
	}
	
	public NBHttpResponse(HttpStatus status, String str) {
		statusCode = status;
		try {
			returnStr = new String(str.getBytes(), "UTF-8");
		}
		catch (Exception e) {
			returnStr = "";
			logger.error("NBHttpResponse catch encode exception. cause:" + e.getCause());
		}
	}
	
	public HttpStatus getStatusCode() {
		return statusCode;
	}
	
	public void setStatusCode(HttpStatus status) {
		statusCode = status;
	}
	
	public String getReturnStr() {
		return returnStr;
	}
	
	public void setReturnStr(String str) {
		try {
			returnStr = new String(str.getBytes(), "UTF-8");
		}
		catch (Exception e) {
			returnStr = "";
			logger.error("NBHttpResponse setReturnStr catch encode exception. cause:" + e.getCause());
		}
	}
}
