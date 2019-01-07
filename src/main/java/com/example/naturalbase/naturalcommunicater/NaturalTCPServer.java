package com.example.naturalbase.naturalcommunicater;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.example.naturalbase.common.NBUtils;

public class NaturalTCPServer extends Thread implements ITcpHandlerProc {

    private ServerSocket serverSocket;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Map<Integer, TCPChannel> deviceTcpChannel;
    private final int BUFFER_SIZE = 4096;
    private boolean isRunning = false;
    private Object objLock = new Object();
    
    private Thread sendThread;
    private Thread receiveThread;
    
    private Object objLockSend = new Object();
    private boolean isSendThreadRunning = false;
    private Object objLockReceive = new Object();
    private boolean isReceiveThreadRunning = false;
    
    private Object objLockSendQueue = new Object();
    private Queue<TcpMessage> sendQueue = new LinkedList<TcpMessage>();
    private Object objLockReceiveQueue = new Object();
	private Queue<TcpMessage> receiveQueue = new LinkedList<TcpMessage>();
	
	private ITcpServerHandlerProc tcpServerHandlerProc;
    
    private static final String MESSAGE_TYPE_DEVICE_ONLINE = "DeviceOnline";

    public NaturalTCPServer(int port){
        deviceTcpChannel = new HashMap<Integer, TCPChannel>();

        try{
            serverSocket = new ServerSocket(port);
        }
        catch(IOException e){
            logger.error("server socket create fail.");
            e.printStackTrace();
        }
	}

	public NaturalTCPServer(int port, ITcpServerHandlerProc handler){
		tcpServerHandlerProc = handler;
        deviceTcpChannel = new HashMap<Integer, TCPChannel>();

        try{
            serverSocket = new ServerSocket(port);
        }
        catch(IOException e){
            logger.error("server socket create fail.");
            e.printStackTrace();
        }
	}
	
	public void setTcpServerHandlerCallback(ITcpServerHandlerProc handler){
		logger.info("TcpServer handler registed!");
		tcpServerHandlerProc = handler;
	}
    
    public void startServer() {
		logger.info("NaturalTCPServer start send/receive thread.");
    	sendThread = new Thread (sendThreadProc);
    	sendThread.start();
    	receiveThread = new Thread(receiveThreadProc);
    	receiveThread.start();
    }
    
    public void send(int deviceId, String message) {
    	TcpMessage tcpMessage = new TcpMessage(deviceId, message);
    	synchronized(objLockSendQueue) {
    		sendQueue.offer(tcpMessage);
    	}
    }
    
    private Runnable sendThreadProc = ()->{
    	synchronized(objLockSend) {
			isSendThreadRunning = true;
			logger.info("TCPServer send thread start.");
    	}
    	while(isSendThreadRunning) {
    		synchronized(objLockSendQueue) {
    			if (!sendQueue.isEmpty()) {
    				TcpMessage msg = sendQueue.poll();
    				if (deviceTcpChannel.containsKey(msg.deviceId)) {
    					deviceTcpChannel.get(msg.deviceId).send(msg.msg.getBytes());
    				}
    			}
    		}
    	}
    };
    
    @Override
	public void onReceive(TcpMessage msg) {
		// TODO Auto-generated method stub
    	synchronized(objLockReceiveQueue) {
			logger.info("TCPServer msg push in queue!");
    		receiveQueue.offer(msg);
    	}
	}
    
    private Runnable receiveThreadProc = ()->{
    	synchronized(objLockReceive) {
			isReceiveThreadRunning = true;
			logger.info("TCPServer receive thread start.");
    	}
    	while(isReceiveThreadRunning){
    		synchronized(objLockReceiveQueue){
				if(!receiveQueue.isEmpty()){
					//logger.info("TCPServer receive queue is not empty");
					TcpMessage msg = receiveQueue.poll();
					if(tcpServerHandlerProc != null){
						tcpServerHandlerProc.onReceiveTcpMessage(msg.deviceId, msg.msg);
					}
					else{
						logger.info("receive tcp message from [" + String.valueOf(msg.deviceId) + "]:" + msg.msg);
					}
				}
			}
    	}
    	
    };
    
    @Override
    public void run() {
    	try {
    		synchronized(objLock) {
				isRunning = true;
				logger.info("Thread[" + Thread.currentThread().getId() + "] tcp server start run!");
    		}
    		while(isRunning) {
    			Socket socket = serverSocket.accept();
    			if (socket.getReceiveBufferSize() > BUFFER_SIZE){
    				socket.setReceiveBufferSize(BUFFER_SIZE);
    			}
    			socket.setTcpNoDelay(true);
    			InputStream in = socket.getInputStream();
    			byte[] inBuf = new byte[BUFFER_SIZE];
    			int len = in.read(inBuf);
    			if (len <= 0) {
    				logger.error("client[" + socket.getRemoteSocketAddress().toString() + "]:do not send device id.");
    				in.close();
    				socket.close();
    				continue;
    			}
				String msg = NBUtils.ToUTF8String(inBuf);
				logger.info("TCPMessage:" + msg);
    			int deviceId = getDeviceIdFromMessage(msg);
    			if (deviceId == -1) {
    				logger.error("can not get device id.");
    				in.close();
    				socket.close();
    				continue;
    			}
				//TODO:ADD DEVICE INTO LIST.
				logger.debug("Device " + String.valueOf(deviceId) + " [" + socket.getRemoteSocketAddress().toString() + "] connect!");
    			TCPChannel channel = new TCPChannel(deviceId, socket, this);
    			channel.start();
    		}
    	}
    	catch(IOException e) {
    		logger.error("client socket create fail.");
    		e.printStackTrace();
		}
		finally{
			try{
				logger.info("TCPServer close.");
				stopServer();
				serverSocket.close();
			}
			catch(IOException e){
				logger.error("serverSocket close catch exception.");
				e.printStackTrace();
			}
		}
    	
	}
	
	@Override
	public void onChannelStatusChange(TCPChannel channel, int status){
		if(status == ITcpHandlerProc.STATUS_ONLINE){
			deviceTcpChannel.put(channel.getRemoteDeviceId(), channel);
			tcpServerHandlerProc.onDeviceOnlineChange(channel.getRemoteDeviceId(), ITcpHandlerProc.STATUS_ONLINE);
		}
		else{
			deviceTcpChannel.remove(channel.getRemoteDeviceId());
			tcpServerHandlerProc.onDeviceOnlineChange(channel.getRemoteDeviceId(), ITcpHandlerProc.STATUS_OFFLINE);
		}
	}
    
    public void stopServer() {
    	synchronized(objLock) {
			isRunning = false;
		}
		synchronized(objLockSend) {
    		isSendThreadRunning = false;
		}
		for (Integer id : deviceTcpChannel.keySet()){
			deviceTcpChannel.get(id).closeChannel();
		}
		synchronized(objLockReceive){
			isReceiveThreadRunning = false;
		}
    }
    
    private int getDeviceIdFromMessage(String msg) {
		JSONObject messageContent = JSONObject.parseObject(msg);
    	if (messageContent == null || messageContent.size() <= 0) {
    		logger.error("can not parse tcp message.");
    		return -1;
    	}
    	
    	JSONObject messageHeaderObj = messageContent.getJSONObject(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER);
    	if (messageHeaderObj == null || messageHeaderObj.size() <= 0) {
    		logger.error("can not parse tcp message header.");
    		return -1;
    	}
    	
		String messageType = messageHeaderObj.getString(NaturalCommunicater.JSON_MESSAGE_HEADER_MESSAGE_TYPE);
		if(messageType == null){
			logger.error("messageType is null");
			return -1;
		}
    	else if (!messageType.equals(MESSAGE_TYPE_DEVICE_ONLINE)) {
    		logger.error("message type is not equals MESSAGE_TYPE_DEVICE_ONLINE.");
    		return -1;
    	}
    	
    	return messageHeaderObj.getIntValue(NaturalCommunicater.JSON_MESSAGE_HEADER_DEVICE_ID);
    }

}


