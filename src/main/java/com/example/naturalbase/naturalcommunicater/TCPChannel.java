package com.example.naturalbase.naturalcommunicater;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.naturalbase.common.NBUtils;

public class TCPChannel extends Thread{
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private ITcpHandlerProc tcpHandlerProc;
    private int remoteDeviceId = -1;
    
    private Object objLockRunning = new Object();
    private boolean isRunning = false;

    private final int BUFFER_SIZE = 4096;

    private byte[] inBuffer = new byte[BUFFER_SIZE];

    public static final int TCP_MESSAGE_TYPE_DEVICE_ONLINE = 0x55;
	public static final int TCP_MESSAGE_TYPE_DATA_CHANGE = 0xAA;
    public static final int TCP_MESSAGE_TYPE_HEART_BEAT = 0x5A;
    
    private static final long HEART_BEAT_TIME = 10 * 1000; //Ms
    private Timer heartBeatTimer = new Timer("HeartBeat");
    private TimerTask heartBeatTimerProc = new TimerTask(){
    
        @Override
        public void run() {
            byte[] heartbeatMessage = new byte[]{(byte)TCP_MESSAGE_TYPE_HEART_BEAT, 0x0};
            logger.debug("Device:" + String.valueOf(remoteDeviceId) + " send heart beat message:" + NBUtils.ByteArrayToHexString(heartbeatMessage));
            send(heartbeatMessage);
        }
    };

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Deprecated
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
        if (tcpHandlerProc != null){
            tcpHandlerProc.onChannelStatusChange(this, ITcpHandlerProc.STATUS_ONLINE);
        }
    }

    public TCPChannel(Socket s, ITcpHandlerProc handler){
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
        logger.debug("Thread:" + String.valueOf(Thread.currentThread().getId()) + " start heart beat timer.");
        heartBeatTimer.schedule(heartBeatTimerProc, 0, HEART_BEAT_TIME);
    }
    
    public int getRemoteDeviceId(){
        return remoteDeviceId;
    }

    public void send(byte[] buffer){
        try{
            if (out == null){
                logger.error("TCPChannel OutputStream is null.");
                return;
            }
			logger.info("TCPChannel send message:" + NBUtils.ByteArrayToHexString(buffer));
            out.write(buffer);
            out.flush();
        }
        catch (IOException e){
            logger.error("TCPChannel send catch exception:" + e.getCause());
            e.printStackTrace();
        }
    }
    
    private void TcpMessageDeviceOnline() throws IOException {
        int messageLength = in.read();
        byte[] buf = new byte[messageLength];
        in.read(buf, 0, messageLength);
        remoteDeviceId = Integer.valueOf(NBUtils.ToUTF8String(buf));
        logger.info("[Device:" + String.valueOf(remoteDeviceId) + " Thread:" + Thread.currentThread().getId() + "] get online.");
        if (tcpHandlerProc != null){
            tcpHandlerProc.onChannelStatusChange(this, ITcpHandlerProc.STATUS_ONLINE);
        }
    }

    private void TcpMessageHeartBeat() throws IOException {
        int messageLength = in.read();
        logger.debug("TimeStamp:" + String.valueOf(NBUtils.GetCurrentTimeStamp()) + 
                     " Thread:" + Thread.currentThread().getId() + 
                     " DeviceId:" + String.valueOf(remoteDeviceId) + 
                     " get HEART_BEAT message! MessageLength=" + String.valueOf(messageLength));
    }

    @Override
    public void run() {
        synchronized(objLockRunning){
            isRunning = true;
            //logger.info("[Device:" + String.valueOf(remoteDeviceId) + " Thread:" + Thread.currentThread().getId() + "] TCPChannel running.");
        }
        try {
            while(isRunning){
                int messageType = in.read();
                if (messageType == -1){
                    isRunning = false;
                    logger.debug("[Thread:" + Thread.currentThread().getId() + "] TCPChannel receive EOF, and break loop!");
                    break;
                }
                switch(messageType){
                    case TCP_MESSAGE_TYPE_DEVICE_ONLINE:
                        logger.debug("TCPChannel get DEVICE_ONLINE message!");
                        TcpMessageDeviceOnline();
                        break;
                    case TCP_MESSAGE_TYPE_HEART_BEAT:
                        TcpMessageHeartBeat();
                        break;
                    default:
                        logger.error("Thread:" + Thread.currentThread().getId() + 
                                     " DeviceId:" + String.valueOf(remoteDeviceId) + 
                                     " invalid message type. type = " + String.format("0x%x", messageType));
                    break;
                }
            }
        }
        catch (SocketTimeoutException e){
            logger.error("TCPChannel catch SocketTimeoutException. DeviceId:" + String.valueOf(remoteDeviceId));
            e.printStackTrace();
        }
        catch(IOException e) {
            logger.error("TCPChannel can not receive tcp message");
            e.printStackTrace();
        }
        finally{
            try{
                logger.info("[Device:" + String.valueOf(remoteDeviceId) + " Thread:" + Thread.currentThread().getId() + "] TCPChannel close.");
                heartBeatTimer.cancel();
                in.close();
                out.close();
                socket.close();
            }
            catch(IOException e){
                logger.error("TcpChannel close socket catch exception. cause=" + e.getCause().toString());
                e.printStackTrace();
            }
            if (tcpHandlerProc != null){
                tcpHandlerProc.onChannelStatusChange(this, ITcpHandlerProc.STATUS_OFFLINE);
            }
        }
    }
    
    public void closeChannel(){
        synchronized(objLockRunning){
            isRunning = false;
        }
        try{
        	heartBeatTimer.cancel();
            in.close();
            out.close();
            socket.close();
        }
        catch(IOException e){
            logger.error("TcpChannel close socket catch exception. cause=" + e.getCause().toString());
            e.printStackTrace();
        }
        if (tcpHandlerProc != null){
            tcpHandlerProc.onChannelStatusChange(this, ITcpHandlerProc.STATUS_OFFLINE);
        }
    }

    public static int getHeartBeatTime(){
        return (int)HEART_BEAT_TIME;
    }
}
