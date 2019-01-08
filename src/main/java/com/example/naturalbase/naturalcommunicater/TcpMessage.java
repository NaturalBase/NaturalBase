package com.example.naturalbase.naturalcommunicater;

import java.io.UnsupportedEncodingException;

public class TcpMessage {
	public int deviceId;
	public byte[] msg;
	
	public TcpMessage(int id, String message) {
		deviceId = id;
		try{
			msg = message.getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e){
			e.printStackTrace();
		}
	}

	public TcpMessage(int id, byte[] message){
		deviceId = id;
		msg = message;
	}
}
