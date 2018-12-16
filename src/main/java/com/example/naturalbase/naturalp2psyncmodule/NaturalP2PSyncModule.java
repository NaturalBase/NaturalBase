package com.example.naturalbase.naturalp2psyncmodule;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.alibaba.fastjson.*;

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
	
	private NaturalCommunicater communicater;
	private NaturalStorage storage;
	
	private Map<Integer, DeviceInfo> deviceMap;
	
	public NaturalP2PSyncModule(NaturalCommunicater inCommunicater, NaturalStorage inStorage){
		communicater = inCommunicater;
		communicater.RegisterIncommingMessageHandler(this);
		storage = inStorage;
		deviceMap = new HashMap<Integer, DeviceInfo>();
	}
	
	public String IncommingMessageHandlerProc(MessageHeader header, JSONObject message) {
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
			//TODO:unknow message type proc
			return "";
		}
	}
	
	private String MessageTimeRequestProc() {
		Date date = new Date();
		long timeStamp = date.getTime();
		
		JSONObject response = new JSONObject();
		JSONObject messageHeader = MakeupMessageHeader(MESSAGE_TYPE_TIME_RESPONSE,
				                                       NaturalCommunicater.JSON_MESSAGE_HEADER_REQUEST_ID_DEFAULT,
				                                       NaturalCommunicater.LOCAL_DEVICE_ID);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER, messageHeader);
		
		JSONObject message = new JSONObject();
		message.put(MESSAGE_TIMESTAMP, timeStamp);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE, message);
		return response.toJSONString();
	}
	
	private String MessageSyncProc(MessageHeader header, JSONObject message) {
		int dataItemSize = message.getIntValue(MESSAGE_DATAITEM_SIZE);
		if (dataItemSize <= 0) {
			System.out.println("message:Sync get dataItemSize <= 0 message.");
			//TODO:dataItemSize <= 0 proc
		}
		
		List<DataItem> dataItemList = new ArrayList<DataItem>();
		JSONArray dataItemArray = message.getJSONArray(MESSAGE_DATAITEM);
		for (int i=0; i<dataItemSize; i++) {
			JSONObject obj = dataItemArray.getJSONObject(i);
			DataItem dataItem = new DataItem();
			dataItem.Key = obj.getString(MESSAGE_KEY);
			dataItem.Value = obj.getString(MESSAGE_VALUE);
			dataItem.TimeStamp = obj.getLongValue(MESSAGE_TIMESTAMP);
			dataItem.DeleteBit = obj.getBooleanValue(MESSAGE_DELETE_BIT);
			dataItemArray.add(dataItem);
		}
		long timeStamp = storage.SaveDataFromSync(dataItemList);
		JSONObject response = new JSONObject();
		JSONObject messageHeader = MakeupMessageHeader(MESSAGE_TYPE_SYNC_ACK,
				                                       NaturalCommunicater.JSON_MESSAGE_HEADER_REQUEST_ID_DEFAULT,
				                                       NaturalCommunicater.LOCAL_DEVICE_ID);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER, messageHeader);
		JSONObject messageObj = new JSONObject();
		messageObj.put(MESSAGE_TIMESTAMP, timeStamp);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE, messageObj);
		return response.toJSONString();
	}
	
	private String MessageRequestSync(MessageHeader header) {
		if (!deviceMap.containsKey(header.deviceId)) {
			System.out.println("message:RequestSync unknow device id id=" + String.valueOf(header.deviceId));
			//TODO:unknow device id proc
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
			dataItemObj.put(MESSAGE_TIMESTAMP, dataItemList.get(i).TimeStamp);
			dataItemObj.put(MESSAGE_DELETE_BIT, dataItemList.get(i).DeleteBit);
			dataItemListArr.add(dataItemObj);
		}
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE, dataItemListArr);
		
		return response.toJSONString();
	}
	
	private String MessageRequestSyncAck(MessageHeader header, JSONObject message) {
		if (!deviceMap.containsKey(header.deviceId)) {
			System.out.println("message:RequestSyncAck unknow device id id=" + String.valueOf(header.deviceId));
			//TODO:unknow device id proc
		}
		
		long newWaterMark = message.getLongValue(MESSAGE_TIMESTAMP);
		deviceMap.get(header.deviceId).waterMark = newWaterMark;
		
		JSONObject response = new JSONObject();
		JSONObject messageHeader = MakeupMessageHeader(MESSAGE_TYPE_RESPONSE_SYNC_ACK,
				                                       NaturalCommunicater.JSON_MESSAGE_HEADER_REQUEST_ID,
				                                       NaturalCommunicater.LOCAL_DEVICE_ID);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER, messageHeader);
		
		JSONObject messageObj = new JSONObject();
		messageObj.put(MESSAGE_RETURN, true);
		response.put(NaturalCommunicater.JSON_OBJECT_MESSAGE, messageObj);
		
		return response.toJSONString();
	}
	
	private void UpdateDeviceMap(int deviceId) {
		Date date = new Date();
		if (deviceMap.containsKey(deviceId)) {
			deviceMap.get(deviceId).lastRequestTimeStamp = date.getTime();
		} else {
			DeviceInfo newDevice = new DeviceInfo();
			newDevice.waterMark = 0;
			newDevice.onlineTimeStamp = date.getTime();
			newDevice.lastRequestTimeStamp = newDevice.onlineTimeStamp;
			deviceMap.put(deviceId, newDevice);
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
