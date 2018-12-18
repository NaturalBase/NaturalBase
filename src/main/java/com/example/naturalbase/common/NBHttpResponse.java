package com.example.naturalbase.common;

import org.springframework.http.HttpStatus;

public class NBHttpResponse {
	private HttpStatus statusCode;
	private String returnStr = new String();
	
	public NBHttpResponse() {
		statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
		returnStr = "server error";
	}
	
	public NBHttpResponse(HttpStatus status, String str) {
		statusCode = status;
		returnStr = str;
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
		returnStr = str;
	}
}
