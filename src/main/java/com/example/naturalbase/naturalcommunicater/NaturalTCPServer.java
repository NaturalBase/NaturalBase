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
    
    public void startServer() {
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
    		receiveQueue.offer(msg);
    	}
	}
    
    private Runnable receiveThreadProc = ()->{
    	synchronized(objLockReceive) {
    		isReceiveThreadRunning = true;
    	}
    	while(isReceiveThreadRunning){
    		
    	}
    	
    };
    
    @Override
    public void run() {
    	try {
    		synchronized(objLock) {
    			isRunning = true;
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
    			int deviceId = getDeviceIdFromMessage(msg);
    			if (deviceId == -1) {
    				logger.error("can not get device id.");
    				in.close();
    				socket.close();
    				continue;
    			}
    			//TODO:ADD DEVICE INTO LIST.
    			TCPChannel channel = new TCPChannel(deviceId, socket, this);
    			channel.start();
    			deviceTcpChannel.put(deviceId, channel);
    		}
    	}
    	catch(IOException e) {
    		logger.error("client socket create fail.");
    		e.printStackTrace();
    	}
    	
    }
    
    public void stopServer() {
    	synchronized(objLock) {
			isRunning = true;
		}
    	isRunning = false;
    }
    
    private int getDeviceIdFromMessage(String msg) {
    	JSONObject messageContent = JSONObject.parseObject(msg);
    	if (messageContent == null) {
    		logger.error("can not parse tcp message.");
    		return -1;
    	}
    	
    	JSONObject messageHeaderObj = messageContent.getJSONObject(NaturalCommunicater.JSON_OBJECT_MESSAGE_HEADER);
    	if (messageHeaderObj == null) {
    		logger.error("can not parse tcp message header.");
    		return -1;
    	}
    	
    	String messageType = messageHeaderObj.getString(NaturalCommunicater.JSON_MESSAGE_HEADER_MESSAGE_TYPE);
    	if (!messageType.equals(MESSAGE_TYPE_DEVICE_ONLINE)) {
    		logger.error("message type is not equals MESSAGE_TYPE_DEVICE_ONLINE.");
    		return -1;
    	}
    	
    	return messageHeaderObj.getIntValue(NaturalCommunicater.JSON_MESSAGE_HEADER_DEVICE_ID);
    }

    public class TCPChannel extends Thread{
        private Socket socket;
        private InputStream in;
        private OutputStream out;
        private ITcpHandlerProc tcpHandlerProc;
        private int remoteDeviceId;

        private final int BUFFER_SIZE = 4096;

        private byte[] inBuffer = new byte[BUFFER_SIZE];

        private Logger logger = LoggerFactory.getLogger(this.getClass());

        public TCPChannel(int id, Socket s, ITcpHandlerProc handler){
            remoteDeviceId = id;
        	socket = s;
            tcpHandlerProc = handler;
            try{
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }
            catch (IOException e){
                logger.error("TCPChannel create fail.");
                e.printStackTrace();
            }
        }

        public void send(byte[] buffer){
            try{
                if (out == null){
                    logger.error("TCPChannel OutputStream is null.");
                    return;
                }
                out.write(buffer);
                out.flush();
            }
            catch (IOException e){
                logger.error("TCPChannel send catch exception:" + e.getCause());
                e.printStackTrace();
            }
        }
        
        @Override
        public void run() {
        	try {
        		int len;
        		while((len = in.read(inBuffer)) != -1){
        			String message = NBUtils.ToUTF8String(inBuffer);
        			TcpMessage tcpMessage = new TcpMessage(remoteDeviceId, message);
        			if (tcpHandlerProc != null) {
        				tcpHandlerProc.onReceive(tcpMessage);
        			}
        		}
        	}
        	catch(Exception e) {
        		logger.error("TCPChannel can not receive tcp message");
        		e.printStackTrace();
        	}
        }
    }
}


