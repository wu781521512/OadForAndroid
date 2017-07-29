package com.example.openoadforandroid.fileutil.manager;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;

import com.example.openoadforandroid.fileutil.BaseFileLoad;

/**
 * 管理类基础接口
 */

public interface BaseManager {

    boolean setNotification(BluetoothGattCharacteristic characteristic,
                            BluetoothGattDescriptor descriptor
            , boolean enabled);

    void queryVersion();

    void startUpdate(BaseFileLoad fileLoad, String filePath, Handler mHandler, int timeInterval);
}
