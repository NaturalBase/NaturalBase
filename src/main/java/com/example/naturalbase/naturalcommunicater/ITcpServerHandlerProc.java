package com.example.naturalbase.naturalcommunicater;

public interface ITcpServerHandlerProc {
    public final static int STATUS_ONLINE = 0;
    public final static int STATUS_OFFLINE = 1;
    public void onReceiveTcpMessage(int deviceId, String message);
    public void onDeviceOnlineChange(int deviceId, int status);
}
