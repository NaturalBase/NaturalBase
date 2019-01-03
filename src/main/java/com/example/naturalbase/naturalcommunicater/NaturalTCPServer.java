package com.example.naturalbase.naturalcommunicater;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalTCPServer extends Thread{

    private ServerSocket serverSocket;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Map<Integer, TCPChannel> deviceTcpChannel;

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

    public class TCPChannel extends Thread{
        private Socket socket;
        private InputStream in;
        private OutputStream out;
        private ITcpHandlerProc tcpHandlerProc;

        private final int BUFFER_SIZE = 4096;

        private byte[] inBuffer = new byte[BUFFER_SIZE];

        private Logger logger = LoggerFactory.getLogger(this.getClass());

        public TCPChannel(Socket s, ITcpHandlerProc handler){
            socket = s;
            tcpHandlerProc = handler;
            try{
                if (socket.getReceiveBufferSize() > BUFFER_SIZE){
                    socket.setReceiveBufferSize(BUFFER_SIZE);
                }
                socket.setTcpNoDelay(true);
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
    }
}


