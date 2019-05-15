package cn.wonderbits.ble;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public class BleScanDevice implements Serializable {
    private int rssi;
    private String name;
    private BluetoothDevice device;

    public BleScanDevice(int rssi, BluetoothDevice device) {
        this.rssi = rssi;
        this.device = device;
        if (device != null) {
            this.name = device.getName();
        }
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public String getName() {
        return name;
    }
}
