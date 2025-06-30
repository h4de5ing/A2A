package com.genymobile.scrcpy;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public final class DesktopConnection implements Closeable {
    private final Socket videoSocket;
    private final SocketChannel videoChannel;

    private DesktopConnection(SocketChannel videoSocket) {
        this.videoSocket = videoSocket.socket();
        videoChannel = videoSocket.socket().getChannel();
    }

    public static DesktopConnection open(SocketChannel videoSocket) {
        return new DesktopConnection(videoSocket);
    }

    public void close() throws IOException {
        videoSocket.shutdownInput();
        videoSocket.shutdownOutput();
        videoSocket.close();
    }

    public SocketChannel getVideoChannel() {
        return videoChannel;
    }
}
