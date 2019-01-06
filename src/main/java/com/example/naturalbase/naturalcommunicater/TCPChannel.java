package com.example.naturalbase.naturalcommunicater;

import java.io.IOException;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.naturalbase.common.NBUtils;

public class TCPChannel extends Thread{
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private ITcpHandlerProc tcpHandlerProc;
    private int remoteDeviceId;
    
    private Object objLockRunning = new Object();
    private boolean isRunning = false;

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
        if (tcpHandlerProc != null){
            tcpHandlerProc.onChannelStatusChange(this, ITcpHandlerProc.STATUS_ONLINE);
        }
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
        synchronized(objLockRunning){
            isRunning = true;
            logger.info("[Device:" + String.valueOf(remoteDeviceId) + " Thread:" + Thread.currentThread().getId() + "] TCPChannel running.");
        }
        try {
            int len;
            while(isRunning && ((len = in.read(inBuffer)) != -1)){
                String message = NBUtils.ToUTF8String(inBuffer);
                logger.info("TCPChannel receive:" + message);
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
        finally{
            try{
                logger.info("[Device:" + String.valueOf(remoteDeviceId) + " Thread:" + Thread.currentThread().getId() + "] TCPChannel close.");
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
