package com.example.naturalbase.naturalcommunicater;

public class MessageHeader {
	public String messageType;
	public String requestId;
	public int deviceId;
	
	MessageHeader(){
		messageType = new String();
		requestId = new String();
	}
}
