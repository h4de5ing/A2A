# Android 2 Android - 安卓互联解决方案

1. USB直连 主从模式
2. 网线直连 Android11以及以上版本支持，USB网络共享
3. 串口直连 2台设备串口直连
4. 互联画面投屏

Andorid 投屏的方式：
1.USB 走adb  
2.WIFI  
3.内网主机发现ndsmanager  
4.DLNA  

参考资料：
```agsl
Android USB

https://cloud.tencent.com/developer/article/2041129  
ya-webadb + websockify
https://www.vysor.io  
网页adb
https://app.tangoapp.dev    
https://yume-chan.github.io/ya-webadb/scrcpy  
https://github.com/yume-chan/ya-webadb   后端可以移植这个库1
https://gitee.com/viarotel-org/escrcpy 基于Desktop js版本  
Android 远程控制与投屏方案  
基于ADB  
https://github.com/Genymobile/scrcpy  
https://github.com/barry-ran/QtScrcpy  
基于网页
https://github.com/openstf/minicap  废弃
https://github.com/DeviceFarmer/minicap 后端可以移植这个库2
https://github.com/openstf/stf  
修改版本
https://testerhome.com/topics/21647  
AServer BClient 修改版本参考源码 
https://gitlab.com/las2mile/scrcpy-android
Android实现录屏直播（三）MediaProjection + VirtualDisplay + librtmp + MediaCodec实现视频编码并推流到rtmp服务器  
https://www.cnblogs.com/raomengyang/p/6544908.html  
https://github.com/myrao/ScreenRecorder

基于WebRTC 屏幕串流
https://github.com/dkrivoruchko/ScreenStream
Android官方的录屏 MediaRecorder
https://github.com/android/media-samples
https://github.com/lesa1127/AndroidScreenShare
https://github.com/yrom/ScreenRecorder
https://github.com/omerjerk/Screenshotter

kmp 跨拼图底层实现库
https://github.com/JetBrains/skiko

java环境进行H264视频解码库
https://github.com/bytedeco/javacv   底层C++实现

http://jcodec.org/
```

## 局域网设备发现
````markdown
1.UDP广播
2.NsdManager
````


# Simplesshd Android开源版ssdh
```markdown

https://github.com/tfonteyn/Sshd4a
源码基于
a85ea6733ad22d31393cf38288182154b7767972
```
