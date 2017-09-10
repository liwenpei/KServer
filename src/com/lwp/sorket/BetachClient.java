package com.lwp.sorket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;


public class BetachClient {
    // 信道选择器
    private Selector selector;
    // 与服务器通信的信道
    SocketChannel socketChannel;

    // 要连接的服务器Ip地址
    private String hostIp;

    // 要连接的远程服务器在监听的端口
    private int hostListenningPort;

    public BetachClient(String HostIp, int HostListenningPort) throws IOException {
        this.hostIp = HostIp;
        this.hostListenningPort = HostListenningPort;

        initialize();
    }

    /**
     * 初始化
     *
     * @throws IOException
     */
    private void initialize()  {
        // 打开监听信道并设置为非阻塞模式
    	try{
    		socketChannel = SocketChannel.open(new InetSocketAddress(hostIp, hostListenningPort));
            socketChannel.configureBlocking(false);

            // 打开并注册选择器到信道
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_READ);

            // 启动读取线程
            new BetachReadThread(selector);
    	}catch(IOException e){}
        
    }

    /**
     * 发送字符串到服务器
     *
     * @param writeBuffer
     * @throws IOException
     */
    public void sendMsg(ByteBuffer writeBuffer) throws IOException {
        if (socketChannel != null && socketChannel.isOpen()) {
            while (writeBuffer.hasRemaining()) {
                socketChannel.write(writeBuffer);
            }
        }
    }

    /**
     * 关闭请求通道
     **/
    public void close() {
        try {
            if (selector != null) {
                selector.close();
            }

            if (socketChannel != null) {
                socketChannel.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public boolean isConnected(){
    	if(socketChannel != null){
    		return socketChannel.isConnected();
    	}
    	return false;
    }

    public ByteBuffer read(SocketChannel sc, int length) throws IOException {
        ByteBuffer rntValue = ByteBuffer.allocate(length);
        int count = 0;
        do {
            int tmpCount = sc.read(rntValue);
            count = count + tmpCount;
        } while (count < length);

        return rntValue;
    }

}

