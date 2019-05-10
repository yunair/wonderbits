package cn.wonderbits.ble;

import android.bluetooth.BluetoothDevice;

public class BleScanDevice {
    private int rssi;
    private BluetoothDevice device;

    public BleScanDevice(int rssi, BluetoothDevice device) {
        this.rssi = rssi;
        this.device = device;
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
}
