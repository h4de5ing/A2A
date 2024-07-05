package org.las2mile.scrcpy;

public interface DataChangeListener {
    void onDataChange(byte[] data);

    void onDisconnect();
}
