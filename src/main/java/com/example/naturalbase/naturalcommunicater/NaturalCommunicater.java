package com.example.naturalbase.naturalcommunicater;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.*;

import com.example.naturalbase.naturalp2psyncmodule.NaturalP2PSyncModule;;

public class NaturalCommunicater {
	
	private static NaturalCommunicater mInstance;
	private NaturalP2PSyncModule p2pSyncModule;
	
	public final String CONTENT_TYPE_JSON = "application/json";
	public static final String JSON_OBJECT_MESSAGE_HEADER = "MessageHeader";
	public static final String JSON_OBJECT_MESSAGE = "Message";
	public static final String JSON_MESSAGE_HEADER_MESSAGE_TYPE = "MessageType";
	public static final String JSON_MESSAGE_HEADER_REQUEST_ID = "RequestId";
	public static final String JSON_MESSAGE_HEADER_DEVICE_ID = "DeviceId";
	public static final String JSON_MESSAGE_HEADER_REQUEST_ID_DEFAULT = "unused";
	public static final int LOCAL_DEVICE_ID = 0;
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
			System.out.println("receive invalid request");
			//TODO: invalid request proc
		}
		
		int contentLength = request.getContentLength();
		String requestBody = new String();
		String responseBody = new String();
		try {
			InputStream in = request.getInputStream();
			byte[] inStreamBuffer = new byte[contentLength+1];
			in.read(inStreamBuffer);
			requestBody = new String(inStreamBuffer);
			System.out.println("body:\r\n"+requestBody);
			JSONObject messageContent = JSONObject.parseObject(requestBody);
			if (messageContent == null) {
				System.out.println("can not parse body content");
				//TODO:can not parse body content proc
			}
			JSONObject messageHeaderObj = messageContent.getJSONObject(JSON_OBJECT_MESSAGE_HEADER);
			if (messageHeaderObj == null) {
				System.out.println("can not parse MessageHeader content");
				//TODO:can not parse MessageHeader content proc
			}
			MessageHeader messageHeader = getMessageHeader(messageHeaderObj);
			JSONObject message = messageContent.getJSONObject(JSON_OBJECT_MESSAGE);
			if (message == null) {
				System.out.println("can not parse Message content");
				//TODO:can not parse Message content proc
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
