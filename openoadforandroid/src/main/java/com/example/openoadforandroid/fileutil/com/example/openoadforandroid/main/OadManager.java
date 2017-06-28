package com.example.openoadforandroid.fileutil.com.example.openoadforandroid.main;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * 主体类，使用时创建该对象，操作整个升级流程
 */

public class OadManager {
    private OadManager oadManager;

    private OadManager() {
    }

    public OadManager newInstance() {
        return InnerOad.oadManager;
    }

    public static class InnerOad {
        public static OadManager oadManager = new OadManager();
    }

    /**
     * 开启或关闭notification，不设置descriptor
     * @param gatt   蓝牙的gatt对象
     * @param characteristic 蓝牙的特征对象
     * @param enabled      是否开启notification标识
     */
    public boolean setNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                   boolean enabled) {
        return setNotification(gatt, characteristic, null, enabled);
    }

    /**
     * 开启或关闭notification
     * @param gatt   蓝牙的gatt对象
     * @param characteristic 蓝牙的特征对象
     * @param descriptor  蓝牙的描述符对象
     * @param enabled      是否开启notification标识
     */
    public boolean setNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                   BluetoothGattDescriptor descriptor, boolean enabled) {
        boolean isSuccess = gatt.setCharacteristicNotification(characteristic, enabled);
        boolean descriptorSuccess = false;
        if (descriptor != null) {
            if (enabled) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }else{
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            descriptorSuccess = gatt.writeDescriptor(descriptor);
        }
        if (descriptor != null) {
            isSuccess = isSuccess && descriptorSuccess;
        }
        return isSuccess;
    }
}
