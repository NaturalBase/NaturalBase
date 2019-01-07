package com.example.naturalbase.naturalcommunicater;

public interface ITcpHandlerProc {
	public final static int STATUS_ONLINE = 0;
    public final static int STATUS_OFFLINE = 1;
	public void onReceive(TcpMessage msg);
	public void onChannelStatusChange(TCPChannel channel, int status);
}
