package com.example.openoadforandroid.fileutil;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

/**
 * 文件加载基类，默认是实现类AssetFileLoad，可以继承这个类实现自己的文件加载
 */

public abstract class BaseFileLoad {
    public abstract byte[] loadFile(String filePath, Context context);
}
