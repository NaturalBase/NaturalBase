package com.example.naturalbase.naturalcommunicater;

public class TcpMessage {
	public int deviceId;
	public String msg;
	
	public TcpMessage(int id, String message) {
		deviceId = id;
		msg = message;
	}
}
