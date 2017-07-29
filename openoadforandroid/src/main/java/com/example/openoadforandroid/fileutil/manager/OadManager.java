package com.example.openoadforandroid.fileutil.manager;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.example.openoadforandroid.fileutil.AssetFileLoad;
import com.example.openoadforandroid.fileutil.BaseFileLoad;

import java.util.UUID;

/**
 * 主体类，使用时创建该对象，操作整个升级流程
 */

public class OadManager implements BaseManager {
    private Context context;
    private BluetoothGatt gatt;
    private int fileSize;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic query_cha;   //查询设备版本和发送升级通知给设备的characteristic
    private BluetoothGattCharacteristic update_cha;  //发送升级文件的characteristic
    private byte[] bytes;
    private OadManager(){}

    public OadManager getInstance(){
        return InnerOad.manager;
    }

    private static class InnerOad{
        public static final OadManager manager= new OadManager();
    }

    /**
     * 初始化升级需要使用的characteristic对象
     * @param gatt  连接蓝牙获取到的BluetoothGatt对象
     * @param commonService 蓝牙升级用到的服务的UUID的字符串
     * @param queryCharacteristicStr  用于查询版本的characteristic的UUID字符串
     * @param updateCharacteristicStr 用于发送升级文件的characteristic的UUID字符串
     * */
    public void initCharacteristic(BluetoothGatt gatt,String commonService,String queryCharacteristicStr ,String updateCharacteristicStr){
        this.gatt = gatt;
        service = gatt.getService(UUID.fromString(commonService));
        query_cha = service.getCharacteristic(UUID.fromString(queryCharacteristicStr));
        update_cha = service.getCharacteristic(UUID.fromString(updateCharacteristicStr));
    }

    public void initCharacteristic(BluetoothGatt gatt,BluetoothGattCharacteristic query_cha ,BluetoothGattCharacteristic update_cha){
        this.gatt = gatt;
        this.query_cha = query_cha;
        this.update_cha = update_cha;
    }

    /**
     * 为查询用的characteristic设置是否开启notification （不设置Descriptor，有可能收不到回执）
     * @param enabled true if open,false is close
     * */
    public boolean setQueryNotification(boolean enabled) {
        return setNotification(query_cha,null,enabled);
    }

    /**
     * 为查询用的characteristic设置是否开启notification 同时设置descriptor 推荐
     * @param descriptor 要设置的Descriptor
     * @param enabled true if open,false is close
     * */
    public boolean setQueryNotification(BluetoothGattDescriptor descriptor,boolean enabled) {
        return setNotification(query_cha,descriptor,enabled);
    }

    /**
     * 为发送升级文件用的characteristic设置是否开启notification
     * @param enabled true if open,false is close
     * */
    public boolean setUpdateNotification(boolean enabled) {
        return setNotification(update_cha,null,enabled);
    }

    /**
     * 为发送升级文件用的characteristic设置是否开启notification 同时设置descriptor 推荐
     * @param descriptor 要设置的Descriptor
     * @param enabled true if open,false is close
     * */
    public boolean setUpdateNotification(BluetoothGattDescriptor descriptor,boolean enabled) {
        return setNotification(update_cha,descriptor,enabled);
    }



    /**
     * 开启或关闭notification
     *
     * @param characteristic 蓝牙的特征对象
     * @param descriptor     蓝牙的描述符对象
     * @param enabled        是否开启notification标识
     */
    public boolean setNotification(BluetoothGattCharacteristic characteristic,
                                   BluetoothGattDescriptor descriptor, boolean enabled) {
        boolean isSuccess = gatt.setCharacteristicNotification(characteristic, enabled);
        boolean descriptorSuccess = false;
        if (descriptor != null) {
            if (enabled) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            descriptorSuccess = gatt.writeDescriptor(descriptor);
        }
        if (descriptor != null) {
            isSuccess = isSuccess && descriptorSuccess;
        }
        return isSuccess;
    }

    /**
     * 查询蓝牙版本，在oncharacteristicChange中接收设备发来的字节信息
     */
    public void queryVersion() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //启动一个线程执行查询任务
                byte[] data = new byte[1];
                data[0] = 0;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //发送0,发送成功后，间隔200ms左右发送1.
                query_cha.setValue(data);
                gatt.writeCharacteristic(query_cha);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //发送1
                data[0] = 1;
                query_cha.setValue(data);
                gatt.writeCharacteristic(query_cha);
            }
        }).start();
    }

    /**
     * 发送升级通知消息给蓝牙设备
     *
     * @param fileLoad 文件加载器，默认从asset文件夹加载，可以自定义实现
     * @param filePath 文件路径，如果用默认的加载器 填写asset文件夹下的文件名称
     */
    private boolean sendUpdateNotify(BaseFileLoad fileLoad, String filePath) {
        BaseFileLoad baseload = fileLoad;
        if (fileLoad == null) {
            baseload = new AssetFileLoad();
        }
        bytes = baseload.loadFile(filePath, context);
        byte[] prepareBuffer = new byte[8 + 2 + 2];
        /**设置要发送的内容，从文件数组下标4将内容复制到prepareBuffer数组中
         * 01 00 00 7C 42 42 42 42一定包含这样类型的字节信息，
         * 其实就是通知蓝牙设备发送的镜像版本01，大小7C（嵌入式那边7C*4就是文件长度，*/
        System.arraycopy(bytes, 4, prepareBuffer, 0, 8);
        //将信息发送到蓝牙设备
        query_cha.setValue(prepareBuffer);
        return gatt.writeCharacteristic(query_cha);
    }

    /**
     * 开始升级
     *
     * @param mHandler 处理升级状态的handler，获取升级进度
     * @param timeInterval 发送升级文件的时间间隔  间隔时间不能太短 默认60ms 太短很容易造成写入失败
     */
    public void startUpdate(BaseFileLoad fileLoad, String filePath, Handler mHandler,int timeInterval) {
        new UpdateTask(fileLoad, filePath, mHandler).execute();
    }

    class UpdateTask extends AsyncTask<Void, Void, Void> {
        BaseFileLoad fileLoad = null;
        String filePath = null;
        Handler mHandler;
        int timeInterval;

        public UpdateTask(BaseFileLoad fileLoad, String filePath, Handler mHandler) {
            this(fileLoad, filePath, mHandler, 0);
        }

        public UpdateTask(BaseFileLoad fileLoad, String filePath, Handler mHandler, int timeInterval) {
            this.fileLoad = fileLoad;
            this.filePath = filePath;
            this.mHandler = mHandler;
            this.timeInterval = timeInterval;
        }

        @Override
        protected Void doInBackground(Void... params) {
            int nowSize = 0;
            boolean over = sendUpdateNotify(fileLoad, filePath);  /**发送升级通知,判断是否发送成功，间隔一段时间开发发送文件*/
            if (over) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.sendEmptyMessage(1);   //发送文件

                /**用于发送升级文件的characteristic在升级过程中会每发送一个包就回复
                 * 一次，很耽误效率，所以最好先设置成不回复消息 */
                update_cha.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                //这里按照demo中设定的一样16，计算一共需要发送多少个包
                int blockNum = bytes.length / 16;
                //计算有没有不满16字节的数据包，有的话加上这个包
                int lastNum = bytes.length % 16;        //检测是否有最后一个不满足16字节的包
                int lastBlockNum = blockNum;       //总包数
                if (lastNum != 0) {
                    lastBlockNum = blockNum + 1;
                }
                /**这里设置的升级锁，如果为真，如果有其他发送给蓝牙的消息一概丢弃，避免对升级造成影响*/
                for (int i = 0; i < lastBlockNum; i++) {
                    //中途失败就取消异步任务，每次都判断下是否取消
                    if (isCancelled()) {
                        return null;
                    }
                    /**最终要发送的包还要加2个头字节，表示发送的包的索引，索引从0开始，索引从0开始，索引从0开始，重要的话说三遍，这是折磨我很久的坑，但是不知道是不是都是需要从0开始，我之前从1开始一直不行，低位在前，高位在后,比如第257个包，temp[0]就是1，temp[1]是1，都是16进制表示，还原到2进制就是0000 0001 0000 0001，这就是头2个字节组合后代表的数字257*/
                    byte[] temp = new byte[2 + 16];
                    //低位
                    temp[0] = (byte) (i & 0xff);
                    //高位
                    temp[1] = (byte) (i >> 8 & 0xff);
                    //每次偏移数据部分大小，将文件数组复制到发送的包中。
                    System.arraycopy(bytes, i * 16, temp, 2, 16);
                    update_cha.setValue(temp);
                    boolean b = gatt.writeCharacteristic(update_cha);
                    if (b) {
                        if (mHandler != null) {
                            //发送写入进度，arg1代表进度
                            Message message = Message.obtain();
                            message.what = 2;
                            message.arg1 = (int) ((i + 1) / (float) lastBlockNum * 100);
                        }
                        try {

//这里需要注意发送的间隔时间 不能太短 不能太短  不能太短！
// demo中默认设置是20ms，但是android实际测试完全不行，后来我改成50ms升级成功了，
// 但是第二天再试又出错了，又改成了60ms，升级有成功了，但是只是在小米试过，
// 其他的不知道60ms会不会出问题，还是ios强大，间隔20ms完全没问题
// 这里的mHandler都是用来更新界面的，比如刷新进度条。
                            if (timeInterval == 0) {
                                timeInterval = 60;
                            }
                            Thread.sleep(timeInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (mHandler != null) {
                            //发送写入错误
                            mHandler.sendEmptyMessage(3);
                        }
                        cancel(true);
                        break;
                    }
                }

            } else {
                mHandler.sendEmptyMessage(4);    //发送升级通知写入错误
                cancel(true);
            }
            return null;
        }
    }
}
