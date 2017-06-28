package com.example.openoadforandroid.fileutil;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * 默认文件加载实现类，从asset文件夹获取文件
 */

public class AssetFileLoad extends BaseFileLoad {
    /**
     * 从asset加载文件，返回文件的字节数组，加载失败返回null
     * **/
    @Override
    public byte[] loadFile(String filePath, Context context) {
        byte[] mFileBuffer = null;
        boolean fSuccess = false;
        int dataSize = 0;
        // Load binary file
        try {
            // Read the file raw into a buffer
            InputStream stream;
            stream = context.getAssets().open(filePath);
            dataSize = stream.available();
            mFileBuffer = new byte[dataSize];
            stream.read(mFileBuffer, 0, dataSize);
            stream.close();
        } catch (IOException e) {
            // Handle exceptions here
            return null;
        }
        return mFileBuffer;
    }
}
