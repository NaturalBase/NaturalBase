package com.example.naturalbase.naturalcommunicater;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

import com.alibaba.fastjson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.naturalbase.common.NBHttpResponse;
import com.example.naturalbase.common.NBUtils;
import com.example.naturalbase.naturalp2psyncmodule.NaturalP2PSyncModule;;

public class NaturalCommunicater {
	
	private static NaturalCommunicater mInstance;
	private NaturalP2PSyncModule p2pSyncModule;
	private NaturalTCPServer tcpServer;
	
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
	private final String RETURN_CODE_SYSTEM_ERROR = "system error";
	
	private final int TCP_PORT = 10086;
	//private final int TCP_PORT = 10087;

	/*
	 * constructed function
	 */
	NaturalCommunicater(){
		tcpServer = new NaturalTCPServer(TCP_PORT);
		tcpServer.start();
		tcpServer.startServer();
	}
	
	public static NaturalCommunicater Instance() {
		if (mInstance == null) {
			mInstance = new NaturalCommunicater();
		}
		return mInstance;
	}
	
	public ResponseEntity<Object> IncommingRequestProc(HttpServletRequest request) {
		
		if (!checkRequestHeader(request)) {
			logger.debug("receive invalid request. Content-Type:" + request.getContentType() +
					" Content-Length:" + request.getContentLength());
			return new ResponseEntity<>(NBUtils.generateErrorInfo(RETURN_CODE_INVALID_REQUEST), HttpStatus.BAD_REQUEST);
		}
		
		int contentLength = request.getContentLength();
		String requestBody = new String();
		NBHttpResponse response = new NBHttpResponse();
		try {
			InputStream in = request.getInputStream();
			byte[] inStreamBuffer = new byte[contentLength];
			in.read(inStreamBuffer);
			requestBody = NBUtils.ToUTF8String(inStreamBuffer);;
			logger.info("body content:"+requestBody, this.getClass());
			
			JSONObject messageContent = JSONObject.parseObject(requestBody);
			if (messageContent == null) {
				logger.error("can not parse body content");
				return new ResponseEntity<>(NBUtils.generateErrorInfo(RETURN_CODE_UNKNOW_CONTENT), HttpStatus.BAD_REQUEST);
			}
			JSONObject messageHeaderObj = messageContent.getJSONObject(JSON_OBJECT_MESSAGE_HEADER);
			if (messageHeaderObj == null) {
				logger.error("can not parse MessageHeader content");
				return new ResponseEntity<>(NBUtils.generateErrorInfo(RETURN_CODE_UNKNOW_MESSAGE_HEADER), HttpStatus.BAD_REQUEST);
			}
			MessageHeader messageHeader = getMessageHeader(messageHeaderObj);
			JSONObject message = messageContent.getJSONObject(JSON_OBJECT_MESSAGE);
			if (message == null) {
				//some message do not have message content, so do not need to proc
				logger.debug("can not parse Message content. MessageType:" + messageHeader.messageType);
			}
			response = MessageHandlerProc(messageHeader, message);
		}
		catch(IOException e) {
			System.out.println("get request body catch exception");
			e.printStackTrace();
		}
		
		return new ResponseEntity<>(response.getReturnStr(), response.getStatusCode());
	}
	
	private boolean checkRequestHeader(HttpServletRequest request) {
		String contentType = request.getContentType();
		int contentLength = request.getContentLength();
		
		if (contentLength > 0 && contentType.contains(CONTENT_TYPE_JSON)) {
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
	
	private NBHttpResponse MessageHandlerProc(MessageHeader header, JSONObject message) {
		if (p2pSyncModule == null) {
			logger.error("incomming message handler do not register!");
			return new NBHttpResponse(HttpStatus.BAD_REQUEST, NBUtils.generateErrorInfo(RETURN_CODE_SYSTEM_ERROR));
		}
		return p2pSyncModule.IncommingMessageHandlerProc(header, message);
	}
	
	public void RegisterIncommingMessageHandler(NaturalP2PSyncModule naturalP2pSyncModule) {
		p2pSyncModule = naturalP2pSyncModule;
	}

	public void RegisterTCPServerHandler(ITcpServerHandlerProc handler){
		tcpServer.setTcpServerHandlerCallback(handler);
	}

	public void SendTcpMessage(int deviceId, String message){
		tcpServer.send(deviceId, message);
	}

	public void SendTcpMessage(int deviceId, byte[] message){
		tcpServer.send(deviceId, message);
	}
}
