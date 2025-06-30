package com.genymobile.scrcpy;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public final class DesktopConnection implements Closeable {
    private final Socket videoSocket;
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
        videoChannel = videoSocket.socket().getChannel();
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
