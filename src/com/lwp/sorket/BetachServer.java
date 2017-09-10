package com.lwp.sorket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.json.JSONObject;

import com.lwp.util.ConvertUtil;

public class BetachServer {
	private static final String TAG = "BetachServer";
	public static final int PC_ANDROID_PORT = 12521;
	ServerSocket server = null;
	private boolean isRun = true;

	public BetachServer() {
		start(PC_ANDROID_PORT);
	}

	void stop() {
		isRun = false;
	}

	void start(int port) {
		try {
			ServerSocketChannel s = ServerSocketChannel.open();
			ServerSocket socket = s.socket();
			final Selector selector = Selector.open();
			InetSocketAddress addr = new InetSocketAddress(port);
			socket.bind(addr);
			s.configureBlocking(false);
			s.register(selector, SelectionKey.OP_ACCEPT);
			new BetachServerThread(selector);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	class BetachServerThread extends Thread {
		private Selector selector;
		public BetachServerThread(Selector selector) {
			this.selector = selector;
			start();
		}
		@Override
		public void run() {
			try {
				while (isRun) {
					int selCount = selector.select();
					if (selCount <= 0)
						continue;

					Iterator<SelectionKey> keys = selector.selectedKeys()
							.iterator();
					while (keys.hasNext()) {
						final SelectionKey key = (SelectionKey) keys.next();
						keys.remove();
						if (key.isAcceptable()) {
							ServerSocketChannel ssc = (ServerSocketChannel) key
									.channel();
							SocketChannel sc = ssc.accept();
							sc.configureBlocking(false);
							sc.register(selector, SelectionKey.OP_READ);
						} else if (key.isReadable()) {
							ByteBuffer resqBuff = null;
							try {
								SocketChannel skChannel = (SocketChannel) key.channel();
								SocketUtil.channelQueue.add(skChannel);
								RequestBean bean = SocketUtil
										.analyzeRequest(skChannel);
								JSONObject data = bean.getJsonData();
								data = (JSONObject)data.get("data");
								String address = data.getString("targetAddress");
								int port  = data.getInt("port");
								BetachClient socketClient;
								if(SocketUtil.clientQueue.containsKey(address + "_" + port)){
									socketClient = SocketUtil.clientQueue.get(address + "_" + port);
								}else{
									socketClient= new BetachClient(address,port);
									SocketUtil.clientQueue.put(address + "_" + port,socketClient);
								}
								if(!socketClient.isConnected()){
									System.out.println("请求连接远程服务器失败");
									SocketUtil.clientQueue.remove(address + "_" + port);
									socketClient = SocketUtil.clientQueue.get(address + "_" + port);
									//连接远程服务器失败
									skChannel = SocketUtil.channelQueue.remove();
									resqBuff = getReponseErrData(1,"连接远程服务器失败");
									if (skChannel != null && skChannel.isOpen() && resqBuff != null) {
										resqBuff.rewind();
										while (resqBuff.hasRemaining()) {
											skChannel.write(resqBuff);
										}
										resqBuff.clear();
									}
								}else{
									socketClient.sendMsg(getByteBufferForJson(bean.getRequestCode(),"Hello"));	
								}
								
								 
								//resqBuff = response((SocketChannel) key.channel(), bean);
								/*if (resqBuff != null) {
									while (resqBuff.hasRemaining()) {
										int count = skChannel.write(resqBuff);
									}
									resqBuff.clear();
								}*/

							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							// 写完后取消可写事件，仅监听可读事件
							// key.interestOps(SelectionKey.OP_READ);

							/*
							 * ByteBuffer bb = resqBuff.duplicate();
							 * key.attach(bb);
							 */

							/*
							 * key.interestOps(SelectionKey.OP_WRITE |
							 * SelectionKey.OP_READ);
							 */

							//key.cancel();
						}
					}
				}

			} catch (Exception ex) {
			}
		}
	}
	
	/** 发送错误数据数据 **/
    public ByteBuffer getReponseErrData(int response, String errMsg) {
        ByteBuffer byteBuff = null;
        try {
            // 请求手机短信、联系人、通话记录的个数
            JSONObject obj = new JSONObject();
            obj.put("return", -1);
            obj.put("reason", errMsg);
            //obj.put("data", new JSONObject());

            int length = obj.toString().getBytes("UTF8").length;

            byteBuff = ByteBuffer.allocate(8 + length);
            // 返回头部位坐标0:响应命令
            byteBuff.put((byte) response);
            // 返回头部位坐标1-4:内容长度
            byteBuff.put(ConvertUtil.intToByteArray(length));
            // 返回头部位坐标5:内容类型
            byteBuff.put((byte) 1);
            // 返回头部位坐标6:拓展字节
            byteBuff.put((byte) 0);
            // 返回头部位坐标7:拓展字节
            byteBuff.put((byte) 0);
            // 返回数据部
            byteBuff.put(obj.toString().getBytes("UTF8"));

            byteBuff.rewind();
        } catch (Exception ex) {
        }

        return byteBuff;
    }
	
	  /**获取Json的ByteBuffer*/
    public ByteBuffer getByteBufferForJson(int requestCode,String json) throws UnsupportedEncodingException {
        byte[] buff = json.getBytes("UTF-8");
        int length = json.getBytes("UTF-8").length;
        ByteBuffer headerBuff = getHeaderData(requestCode,length,1,1,1 );
        headerBuff = appendBytes(headerBuff,buff);
        headerBuff.rewind();
        headerBuff.limit(headerBuff.capacity());
        return headerBuff;
    }
    
    public ByteBuffer getHeaderData(int requestCode,int length,int type,int extra1,int extra2){
        //添加头部包头信息
        ByteBuffer headerByte = ByteBuffer.allocate(8);
        //////////////////添加头部信息/////////////////////////
        // 返回头部位坐标0:响应命令
        headerByte.put((byte) requestCode);
        // 返回头部位坐标1-4:内容长度
        headerByte.put(ConvertUtil.intToByteArray(length));
        // 返回头部位坐标5:内容类型
        headerByte.put((byte) type);
        // 返回头部位坐标6:拓展字节
        headerByte.put((byte) extra1);
        // 返回头部位坐标7:拓展字节
        headerByte.put((byte) extra2);
        headerByte.rewind();
        return headerByte;
    }
    
    public ByteBuffer appendBytes(ByteBuffer byteBuffer, byte[] buff){
        if(byteBuffer != null){
            ByteBuffer tmpBuff = ByteBuffer.allocate(byteBuffer.capacity() + buff.length );
            tmpBuff.put(byteBuffer);
            tmpBuff.put(buff);
            return tmpBuff;
        }

        return byteBuffer;
    }
}
