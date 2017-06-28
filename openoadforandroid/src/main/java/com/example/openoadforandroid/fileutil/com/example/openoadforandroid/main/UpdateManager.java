package com.example.openoadforandroid.fileutil.com.example.openoadforandroid.main;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;

import com.example.openoadforandroid.fileutil.BaseFileLoad;

/**
 * 升级管理类
 */

public class UpdateManager implements BaseManager{
    private OadManager oadManager;
    public UpdateManager(BluetoothGatt gatt, Context context){
        oadManager = new OadManager(gatt,context);
    }

    @Override
    public boolean setNotification(BluetoothGattCharacteristic characteristic,
                                BluetoothGattDescriptor descriptor, boolean enabled) {
        return oadManager.setNotification(characteristic,descriptor,enabled);
    }

    @Override
    public void queryVersion() {
        oadManager.queryVersion();
    }

    @Override
    public void startUpdate(BaseFileLoad fileLoad, String filePath, Handler mHandler, int timeInterval) {
        oadManager.startUpdate(fileLoad,filePath,mHandler,timeInterval);
    }


}
