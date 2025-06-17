package com.genymobile.scrcpy;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public final class DesktopConnection implements Closeable {

    private static final int DEVICE_NAME_FIELD_LENGTH = 64;

    private static final String SOCKET_NAME = "scrcpy";
    private static final int SOCKET_PORT = 6612;

    private final Socket videoSocket;
    private final FileDescriptor videoFd;
    private final SocketChannel videoChannel;

    private final Socket controlSocket;
    private final InputStream controlInputStream;
    private final OutputStream controlOutputStream;

    private final ControlMessageReader reader = new ControlMessageReader();
    private final DeviceMessageWriter writer = new DeviceMessageWriter();

    private DesktopConnection(SocketChannel videoSocket, SocketChannel controlSocket) throws IOException {
        this.videoSocket = videoSocket.socket();
        this.controlSocket = controlSocket.socket();
        controlInputStream = controlSocket.socket().getInputStream();
        controlOutputStream = controlSocket.socket().getOutputStream();
//        videoFd = videoSocket.getFileDescriptor();
        videoFd = null;//no use
        videoChannel = videoSocket.socket().getChannel();
        Ln.i("videoChannel: " + videoChannel);
    }

    private static LocalSocket connect(String abstractName) throws IOException {
        LocalSocket localSocket = new LocalSocket();
        localSocket.connect(new LocalSocketAddress(abstractName));
        return localSocket;
    }

    public static DesktopConnection open(Device device, SocketChannel videoSocket, SocketChannel controlSocket) throws IOException {
        return new DesktopConnection(videoSocket, controlSocket);
    }

    public void close() throws IOException {
        videoSocket.shutdownInput();
        videoSocket.shutdownOutput();
        videoSocket.close();
        controlSocket.shutdownInput();
        controlSocket.shutdownOutput();
        controlSocket.close();
    }


    public FileDescriptor getVideoFd() {
        return videoFd;
    }

    public SocketChannel getVideoChannel() {
        return videoChannel;
    }

    public ControlMessage receiveControlMessage() throws IOException {
        ControlMessage msg = reader.next();
        while (msg == null) {
            reader.readFrom(controlInputStream);
            msg = reader.next();
        }
        return msg;
    }

    public void sendDeviceMessage(DeviceMessage msg) throws IOException {
        writer.writeTo(msg, controlOutputStream);
    }
}
