package com.flowchat.app.shizuku;

interface IFlowChatShizukuService {
    void destroy() = 16777114;
    String probeCapabilities() = 1;
    String getDeviceStatus() = 2;
    String getForegroundApp() = 3;
    String setScreenBrightness(int percent) = 4;
    String setMediaVolume(int percent) = 5;
    String forceStopPackage(String packageName) = 6;
}
