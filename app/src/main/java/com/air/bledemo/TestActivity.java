package com.air.bledemo;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import cn.wonderbits.usb.IConnectCallback;
import cn.wonderbits.usb.WBUsb;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TestActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WBUsb.init(this).setDebuggable(true);
        List<UsbSerialDriver> drivers = WBUsb.get().search();
        WBUsb.get().connect(drivers.get(0), new IConnectCallback() {
            @Override
            public void onFailed(@NotNull String msg) {

            }

            @Override
            public void onConnected() {

            }
        });

        WBUsb.get().close();
    }
}
