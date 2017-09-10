package com.lwp.sorket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;

import org.json.JSONObject;

import com.lwp.util.ConvertUtil;


/**
 * Created by Administrator on 2017/9/9.
 */

public class SocketUtil {
	  /*** 头部长度 **/
    public static final int HEADER_LENGTH = 8;
    /** 类型 ***/
    public static final int TYPE_JSON = 0x01;
    public static final int TYPE_PNG = 0x02;
    public static final int TYPE_JPG = 0x03;
    public static final int TYPE_CSV = 0x04;
    public static final int TYPE_VCF = 0x05;
    public static final int TYPE_MP3 = 0x06;
    public static final int TYPE_MP4 = 0x07;
    public static final int TYPE_APK = 0X08;
    public static final int TYPE_BYTE = 0x09;
    public static final int SIZE = 8;
    public static LinkedList<SocketChannel> channelQueue = new LinkedList<SocketChannel>();
    public static HashMap<String,BetachClient> clientQueue = new HashMap<String,BetachClient>();

    public static ByteBuffer read(SocketChannel sc, int length) throws IOException {
        if(sc == null || !sc.isConnected()){
            return null;
        }

        ByteBuffer rntValue = ByteBuffer.allocate(length);
        int count = 0;
        do {
            int tmpCount = sc.read(rntValue);
            if(tmpCount < 0){
                //已经断开
                return null;
            }
            count = count + tmpCount;
        } while (count < length);

        return rntValue;
    }
    
    /**
     * 解析请求
     */
    public static RequestBean analyzeRequest(SocketChannel sc) {
        //SocketChannel sc = (SocketChannel) key.channel();
        RequestBean bean = new RequestBean();
        try {
            //int count = sc.read(buffer);
            ByteBuffer buffer = SocketUtil.read(sc, SIZE);
            if (buffer == null) {
                //通道已经关闭
                sc.close();
                return bean;
            }
            System.out.println("接收到地址" + sc.socket().getRemoteSocketAddress() + "的信息" + ",头部信息：" + buffer.array());
            // 解析头部
            int requestCode = buffer.get(0) & 0xff; // 0
            byte[] dst = {buffer.get(1), buffer.get(2), buffer.get(3),
                    buffer.get(4)};
            int length = ConvertUtil.byteArrayToint(dst); // 1-4
            int type = buffer.get(5) & 0xff; // 5

            byte[] dstReserve = {buffer.get(6), buffer.get(7)};
            int bUsReserve = ConvertUtil.byteArrayToint(dstReserve); // 6-7
            //赋值头部信息
            bean.setRequestCode(requestCode);
            bean.setType(type);
            bean.setExpand(bUsReserve);
            JSONObject json = null;
            if (length > 0) {
                buffer = SocketUtil.read(sc, length);
                bean.setData(buffer);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            try {
                if (sc != null) {
                    sc.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return bean;
    }
}
