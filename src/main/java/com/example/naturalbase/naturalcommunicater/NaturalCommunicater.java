package com.example.naturalbase.naturalcommunicater;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.naturalbase.common.NBLogger;
import com.example.naturalbase.naturalp2psyncmodule.NaturalP2PSyncModule;;

public class NaturalCommunicater {
	
	private static NaturalCommunicater mInstance;
	private NaturalP2PSyncModule p2pSyncModule;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public final String CONTENT_TYPE_JSON = "application/json";
	public static final String JSON_OBJECT_MESSAGE_HEADER = "MessageHeader";
	public static final String JSON_OBJECT_MESSAGE = "Message";
	public static final String JSON_MESSAGE_HEADER_MESSAGE_TYPE = "MessageType";
	public static final String JSON_MESSAGE_HEADER_REQUEST_ID = "RequestId";
	public static final String JSON_MESSAGE_HEADER_DEVICE_ID = "DeviceId";
	public static final String JSON_MESSAGE_HEADER_REQUEST_ID_DEFAULT = "unused";
	public static final int LOCAL_DEVICE_ID = 0;
	
	private final String RETURN_CODE_INVALID_REQUEST = "invalid request";
	private final String RETURN_CODE_UNKNOW_CONTENT = "unknow content";
	private final String RETURN_CODE_UNKNOW_MESSAGE_HEADER = "unknow message header";
	/*
	 * constructed function
	 */
	NaturalCommunicater(){
		
	}
	
	public static NaturalCommunicater Instance() {
		if (mInstance == null) {
			mInstance = new NaturalCommunicater();
		}
		return mInstance;
	}
	
	public String IncommingRequestProc(HttpServletRequest request) {
		
		if (!checkRequestHeader(request)) {
			logger.debug("receive invalid request. Content-Type:%s Content-Length:%d", 
					request.getContentType(), request.getContentLength());
			return RETURN_CODE_INVALID_REQUEST;
		}
		
		int contentLength = request.getContentLength();
		String requestBody = new String();
		String responseBody = new String();
		try {
			InputStream in = request.getInputStream();
			byte[] inStreamBuffer = new byte[contentLength+1];
			in.read(inStreamBuffer);
			requestBody = new String(inStreamBuffer);
			NBLogger.info("body:"+requestBody, this.getClass());
			JSONObject messageContent = JSONObject.parseObject(requestBody);
			if (messageContent == null) {
				logger.debug("can not parse body content");
				return RETURN_CODE_UNKNOW_CONTENT;
			}
			JSONObject messageHeaderObj = messageContent.getJSONObject(JSON_OBJECT_MESSAGE_HEADER);
			if (messageHeaderObj == null) {
				logger.error("can not parse MessageHeader content");
				return RETURN_CODE_UNKNOW_MESSAGE_HEADER;
			}
			MessageHeader messageHeader = getMessageHeader(messageHeaderObj);
			JSONObject message = messageContent.getJSONObject(JSON_OBJECT_MESSAGE);
			if (message == null) {
				//some message do not have message content, so do not need to proc
				logger.debug("can not parse Message content");
			}
			responseBody = MessageHandlerProc(messageHeader, message);
		}
		catch(IOException e) {
			System.out.println("get request body catch exception");
			e.printStackTrace();
		}
		
		return responseBody;
	}
	
	private boolean checkRequestHeader(HttpServletRequest request) {
		String contentType = request.getContentType();
		int contentLength = request.getContentLength();
		
		if (contentLength > 0 && contentType.equals(CONTENT_TYPE_JSON)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private MessageHeader getMessageHeader(JSONObject obj) {
		MessageHeader messageHeader = new MessageHeader();
		messageHeader.messageType = obj.getString(JSON_MESSAGE_HEADER_MESSAGE_TYPE);
		messageHeader.requestId = obj.getString(JSON_MESSAGE_HEADER_REQUEST_ID);
		messageHeader.deviceId = obj.getIntValue(JSON_MESSAGE_HEADER_DEVICE_ID);
		
		return messageHeader;
	}
	
	private String MessageHandlerProc(MessageHeader header, JSONObject message) {
		//TODO:CALL P2P SYNC MODULE TO PROC
		return p2pSyncModule.IncommingMessageHandlerProc(header, message);
	}
	
	public void RegisterIncommingMessageHandler(NaturalP2PSyncModule naturalP2pSyncModule) {
		p2pSyncModule = naturalP2pSyncModule;
	}
}
