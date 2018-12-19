package com.example.naturalbase.naturalp2psyncmodule;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import com.alibaba.fastjson.*;
import com.example.naturalbase.common.NBHttpResponse;
import com.example.naturalbase.common.NBUtils;
import com.example.naturalbase.naturalcommunicater.MessageHeader;
import com.example.naturalbase.naturalcommunicater.NaturalCommunicater;
import com.example.naturalbase.naturalstorage.DataItem;
import com.example.naturalbase.naturalstorage.NaturalStorage;

public class NaturalP2PSyncModule {
	public static final String MESSAGE_TYPE_TIME_REQUEST = "TimeRequest";
	public static final String MESSAGE_TYPE_TIME_RESPONSE = "TimeResponse";
	
	public static final String MESSAGE_TYPE_SYNC = "Sync";
	public static final String MESSAGE_TYPE_SYNC_ACK = "SyncAck";
	
	public static final String MESSAGE_TYPE_REQUEST_SYNC = "RequestSync";
	public static final String MESSAGE_TYPE_RESPONSE_SYNC = "ResponseSync";
	
	public static final String MESSAGE_TYPE_REQUEST_SYNC_ACK = "RequestSyncAck";
	public static final String MESSAGE_TYPE_RESPONSE_SYNC_ACK = "ResponseSyncAck";
	
	public static final String MESSAGE_TIMESTAMP = "TimeStamp"; 
	public static final String MESSAGE_DATAITEM_SIZE = "DataItemSize";
	public static final String MESSAGE_DATAITEM = "DataItem";
	public static final String MESSAGE_KEY = "Key";
	public static final String MESSAGE_VALUE = "Value";
	public static final String MESSAGE_DELETE_BIT = "DeleteBit";
	public static final String MESSAGE_RETURN = "Return";
	
	private final String RETURN_CODE_UNKNOW_MESSAGE_TYPE = "unknow message type";
	private final String RETURN_CODE_INVALID_DATAITEM_SIZE = "invalid dataitemsize";
	private final String RETURN_CODE_INVALID_DATAITEM = "invalid dataitem";
	private final String RETURN_CODE_UNKNOW_DEVICE = "unknow device";
	private final String RETURN_CODE_INVALID_TIMESTAMP = "invalid timestamp";
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private NaturalCommunicater communicater;
	private NaturalStorage storage;
	
	private Map<Integer, DeviceInfo> deviceMap;
	
	public NaturalP2PSyncModule(NaturalCommunicater inCommunicater, NaturalStorage inStorage){
		communicater = inCommunicater;
		communicater.RegisterIncommingMessageHandler(this);
		storage = inStorage;
		deviceMap = new HashMap<Integer, DeviceInfo>();
	}
	
	public NBHttpResponse IncommingMessageHandlerProc(MessageHeader header, JSONObject message) {
		logger.debug("Incomming Message! MessageType:" + header.messageType + " DeviceId:" + header.deviceId);
		UpdateDeviceMap(header.deviceId);
		if (header.messageType.equals(MESSAGE_TYPE_TIME_REQUEST)) {
			return MessageTimeRequestProc();
		}
		else if (header.messageType.equals(MESSAGE_TYPE_SYNC)) {
			return MessageSyncProc(header, message);
		}
		else if (header.messageType.equals(MESSAGE_TYPE_REQUEST_SYNC)) {
			return MessageRequestSync(header);
		}
		else if (header.messageType.equals(MESSAGE_TYPE_REQUEST_SYNC_ACK)) {
			return MessageRequestSyncAck(header, message);
		}
		else {
			return new NBHttpResponse(HttpStatus.BAD_REQUEST, NBUtils.generateErrorInfo(RETURN_CODE_UNKNOW_MESSAGE_TYPE));
		}
	}
	
	private NBHttpResponse MessageTimeRequestProc() {
		Date date = new Date();
		long timeStamp = date.getTime();
		
		JSONObject response = new JSONObject();
		JSONObject messageHeader = MakeupMessageHeader(MESSAGE_TYPE_TIME_RESPONSE,
				                                       NaturalCommunicater.JSON_MESSAGE_HEADER_REQUEST_ID_DEFAULT,
				                                       NaturalCommunicater.LOCAL_DEVICE_ID);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER, messageHeader);
		
		JSONObject message = new JSONObject();
		message.put(MESSAGE_TIMESTAMP, String.valueOf(timeStamp));
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE, message);
		return new NBHttpResponse(HttpStatus.OK, response.toJSONString());
	}
	
	private NBHttpResponse MessageSyncProc(MessageHeader header, JSONObject message) {
		int dataItemSize = message.getIntValue(MESSAGE_DATAITEM_SIZE);
		if (dataItemSize <= 0) {
			logger.error("message:Sync get dataItemSize <= 0 message.");
			return new NBHttpResponse(HttpStatus.BAD_REQUEST, NBUtils.generateErrorInfo(RETURN_CODE_INVALID_DATAITEM_SIZE));
		}
		
		List<DataItem> dataItemList = new ArrayList<DataItem>();
		JSONArray dataItemArray = message.getJSONArray(MESSAGE_DATAITEM);
		if (dataItemArray == null) {
			logger.error("message:Sync can not get DATAITEM");
			return new NBHttpResponse(HttpStatus.BAD_REQUEST, NBUtils.generateErrorInfo(RETURN_CODE_INVALID_DATAITEM));
		}
		for (int i=0; i<dataItemSize; i++) {
			JSONObject obj = dataItemArray.getJSONObject(i);
			DataItem dataItem = new DataItem();
			dataItem.Key = obj.getString(MESSAGE_KEY);
			dataItem.Value = obj.getString(MESSAGE_VALUE);
			dataItem.TimeStamp = Long.parseLong(obj.getString(MESSAGE_TIMESTAMP));
			dataItem.DeleteBit = obj.getBooleanValue(MESSAGE_DELETE_BIT);
			dataItemList.add(dataItem);
		}
		long timeStamp = storage.SaveDataFromSync(dataItemList);
		JSONObject response = new JSONObject();
		JSONObject messageHeader = MakeupMessageHeader(MESSAGE_TYPE_SYNC_ACK,
				                                       NaturalCommunicater.JSON_MESSAGE_HEADER_REQUEST_ID_DEFAULT,
				                                       NaturalCommunicater.LOCAL_DEVICE_ID);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER, messageHeader);
		JSONObject messageObj = new JSONObject();
		messageObj.put(MESSAGE_TIMESTAMP, String.valueOf(timeStamp));
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE, messageObj);
		return new NBHttpResponse(HttpStatus.OK, response.toJSONString());
	}
	
	private NBHttpResponse MessageRequestSync(MessageHeader header) {
		if (!deviceMap.containsKey(header.deviceId)) {
			logger.error("message:RequestSync unknow device id id=" + String.valueOf(header.deviceId));
			return new NBHttpResponse(HttpStatus.BAD_REQUEST, NBUtils.generateErrorInfo(RETURN_CODE_UNKNOW_DEVICE));
		}
		
		DeviceInfo device = deviceMap.get(header.deviceId);
		List<DataItem> dataItemList = storage.GetUnsyncData(device.waterMark, NaturalStorage.TIMESTAMP_NOW);
		
		JSONObject response = new JSONObject();
		JSONObject messageHeader = MakeupMessageHeader(MESSAGE_TYPE_RESPONSE_SYNC,
				                                       NaturalCommunicater.JSON_MESSAGE_HEADER_REQUEST_ID_DEFAULT,
				                                       NaturalCommunicater.LOCAL_DEVICE_ID);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER, messageHeader);
		
		JSONObject messageObj = new JSONObject();
		messageObj.put(MESSAGE_DATAITEM_SIZE, dataItemList.size());
		JSONArray dataItemListArr = new JSONArray();
		for (int i=0; i<dataItemList.size(); i++) {
			JSONObject dataItemObj = new JSONObject();
			dataItemObj.put(MESSAGE_KEY, dataItemList.get(i).Key);
			dataItemObj.put(MESSAGE_VALUE, dataItemList.get(i).Value);
			dataItemObj.put(MESSAGE_TIMESTAMP, String.valueOf(dataItemList.get(i).TimeStamp));
			dataItemObj.put(MESSAGE_DELETE_BIT, dataItemList.get(i).DeleteBit);
			dataItemListArr.add(dataItemObj);
		}
		messageObj.put(MESSAGE_DATAITEM, dataItemListArr);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE, messageObj);
		
		return new NBHttpResponse(HttpStatus.OK, response.toJSONString());
	}
	
	private NBHttpResponse MessageRequestSyncAck(MessageHeader header, JSONObject message) {
		if (!deviceMap.containsKey(header.deviceId)) {
			logger.error("message:RequestSyncAck unknow device id id=" + String.valueOf(header.deviceId));
			return new NBHttpResponse(HttpStatus.BAD_REQUEST, NBUtils.generateErrorInfo(RETURN_CODE_UNKNOW_DEVICE));
		}
		
		if (!message.containsKey(MESSAGE_TIMESTAMP)) {
			logger.error("message:RequestSyncAck message do not contain TIMESTAMP");
			return new NBHttpResponse(HttpStatus.BAD_REQUEST, NBUtils.generateErrorInfo(RETURN_CODE_INVALID_TIMESTAMP));
		}
		long newWaterMark = Long.parseLong(message.getString(MESSAGE_TIMESTAMP));
		deviceMap.get(header.deviceId).waterMark = newWaterMark;
		
		JSONObject response = new JSONObject();
		JSONObject messageHeader = MakeupMessageHeader(MESSAGE_TYPE_RESPONSE_SYNC_ACK,
				                                       NaturalCommunicater.JSON_MESSAGE_HEADER_REQUEST_ID,
				                                       NaturalCommunicater.LOCAL_DEVICE_ID);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER, messageHeader);
		
		JSONObject messageObj = new JSONObject();
		messageObj.put(MESSAGE_RETURN, true);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE, messageObj);
		
		return new NBHttpResponse(HttpStatus.OK, response.toJSONString());
	}
	
	private void UpdateDeviceMap(int deviceId) {
		Date date = new Date();
		if (deviceMap.containsKey(deviceId)) {
			deviceMap.get(deviceId).lastRequestTimeStamp = date.getTime();
			logger.debug("UpdateDevice DeviceId:" + deviceId +
					     " WaterMark:" + deviceMap.get(deviceId).waterMark +
					     " onlineTimeStamp:" + deviceMap.get(deviceId).onlineTimeStamp +
					     " lastRequestTimeStamp:" + deviceMap.get(deviceId).lastRequestTimeStamp);
		} else {
			DeviceInfo newDevice = new DeviceInfo();
			newDevice.waterMark = 0;
			newDevice.onlineTimeStamp = date.getTime();
			newDevice.lastRequestTimeStamp = newDevice.onlineTimeStamp;
			deviceMap.put(deviceId, newDevice);
			logger.debug("UpdateDevice new device online! DeviceId:" + deviceId +
				     " WaterMark:" + deviceMap.get(deviceId).waterMark +
				     " onlineTimeStamp:" + deviceMap.get(deviceId).onlineTimeStamp +
				     " lastRequestTimeStamp:" + deviceMap.get(deviceId).lastRequestTimeStamp);
		}
	}
	
	private JSONObject MakeupMessageHeader(String messageType, String requestId, int deviceId) {
		JSONObject messageHeader = new JSONObject();
		messageHeader.put(NaturalCommunicater.JSON_MESSAGE_HEADER_MESSAGE_TYPE, messageType);
		messageHeader.put(NaturalCommunicater.JSON_MESSAGE_HEADER_REQUEST_ID, requestId);
		messageHeader.put(NaturalCommunicater.JSON_MESSAGE_HEADER_DEVICE_ID, deviceId);
		
		return messageHeader;
	}
}
