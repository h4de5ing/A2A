//package com.android.wifiap.other;
//
//
//import android.content.Context;
//import android.net.wifi.SoftApCapability;
//import android.net.wifi.SoftApInfo;
//import android.net.wifi.WifiClient;
//import android.net.wifi.WifiManager;
//import android.os.Handler;
//import android.os.HandlerExecutor;
//import android.os.Looper;
//
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//import java.util.List;
//import java.util.concurrent.Executor;
//
//public class SoftApCallback {
//
//    public static void registerSoftApCallback(Context context) {
//        try {
//            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//            Class<?> callbackClass = Class.forName("android.net.wifi.WifiManager$SoftApCallback");
//            Object callback = Proxy.newProxyInstance(callbackClass.getClassLoader(), new Class<?>[]{callbackClass}, new InvocationHandler() {
//                @Override
//                public Object invoke(Object proxy, Method method, Object[] args) {
//                    System.out.println("实现接口方法逻辑 " + method.getName());
//                    switch (method.getName()) {
//                        case "onStateChanged":
//                            int state = (int) args[0];
//                            int failureReason = (int) args[1];
//                            System.out.println("onStateChanged ,state=" + state + ",failureReason=" + failureReason);
//                            break;
//
//                        case "onConnectedClientsChanged":
//                            List<WifiClient> clients = (List<WifiClient>) args[0];
//                            System.out.println("onConnectedClientsChanged = " + clients.size());
//                            break;
//
//                        case "onInfoChanged":
//                            if (args[0] instanceof SoftApInfo) {
//                                SoftApInfo softApInfo = (SoftApInfo) args[0];
//                                System.out.println("onInfoChanged = " + softApInfo.toString());
//                            }
//                            break;
//                        case "onCapabilityChanged":
//                            if (args[0] instanceof SoftApCapability) {
//                                SoftApCapability softApCapability = (SoftApCapability) args[0];
//                                System.out.println("onCapabilityChanged = " + softApCapability.toString());
//                            }
//                            break;
//                        case "onBlockedClientConnecting":
//                            WifiClient client = (WifiClient) args[0];
//                            int blockedReason = (int) args[1];
//                            System.out.println("blockedReason ,blockedReason=" + blockedReason);
//                            break;
//                    }
//                    return null;
//                }
//            });
//            Method method = WifiManager.class.getDeclaredMethod("registerSoftApCallback", Executor.class, callbackClass);
//            method.invoke(wifiManager, new HandlerExecutor(new Handler(Looper.getMainLooper())), callback);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
