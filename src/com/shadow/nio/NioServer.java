package com.shadow.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class NioServer {

    public static void main(String[] arg) throws IOException {
        new NioServer().start();
    }

    private void start() throws IOException {
        /**
         * 1. 创建Selector
         */
        Selector selector = Selector.open();
        /**
         * 2. 通过ServerSocketChannel创建channel通道
         */
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        /**
         * 3. 为channel通道绑定监听端口
         */
        serverSocketChannel.bind(new InetSocketAddress(8888));
        /**
         * 4. **设置channel为非阻塞模式**
         */
        serverSocketChannel.configureBlocking(false);
        /**
         * 5. 将channel注册到selector上，监听连接事件
         */
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器启动成功！！！");
        /**
         * 6. 循环等待新连接接入
         */
        while (true) {
            /**
             *  TODO 获取可用channel数量
             */
            int readyChannels = selector.select();
            /**
             * TODO 为什么要这么做？
             */
            if (readyChannels == 0) {
                continue;
            }
            /**
             * 获取可用channel的集合
             */
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (Iterator<SelectionKey> itr = selectionKeys.iterator(); itr.hasNext(); ) {
                SelectionKey selectionKey = itr.next();
                /**
                 * **移除当前Set中的SelectionKey**
                 */
                itr.remove();
                /**
                 * 7. 根据就绪状态，调用对应方法处理业务逻辑
                 */
                // 7.1 如果是接入事件
                if (selectionKey.isAcceptable()) {
                    acceptHandler(serverSocketChannel, selector);
                }
                // 7.2 如果是可读事件
                if (selectionKey.isReadable()) {
                    readHandler(selectionKey, selector);
                }
            }
        }
    }

    /**
     * 接入事件处理器
     */
    private void acceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        // 如果要是接入事件，就创建socketChannel
        SocketChannel socketChannel = serverSocketChannel.accept();
        // 将socketChannel设置为非阻塞的工作模式
        socketChannel.configureBlocking(false);
        // 将socketChannel注册到selector上，监听可读事件
        socketChannel.register(selector, SelectionKey.OP_READ);
        // 回复客户端提示信息
        socketChannel.write(StandardCharsets.UTF_8.encode("您已经成功加入聊天室！！！"));
    }

    /**
     * 可读事件处理器
     */
    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        // 要从selectionKey中获取到已经就绪的channel
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        // 创建buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        // 循环读取客户端请求信息
        String requestMsg = "";

        while (socketChannel.read(byteBuffer) > 0) {
            // 切换buffer为读模式
            byteBuffer.flip();
            // 读取buffer中的内容
            requestMsg += StandardCharsets.UTF_8.decode(byteBuffer);
        }
        // 将channel再次注册到selector上，监听其他可读事件
        socketChannel.register(selector, SelectionKey.OP_READ);
        // 将客户端发送的请求信息 广播给其他客户端
        if (requestMsg.length() != 0) {
            // 广播给其他客户端
            broadCast(selector, socketChannel, requestMsg);
        }
    }

    /**
     * 广播给其他客户端
     */
    private void broadCast(Selector selector,
                           SocketChannel sourceChannel, String request) {
        /**
         * 获取到所有已接入的客户端channel
         */
        Set<SelectionKey> selectionKeySet = selector.keys();
        /**
         * 循环向所有channel广播信息
         */
        selectionKeySet.forEach(selectionKey -> {
            Channel targetChannel = selectionKey.channel();
            // 剔除发消息的客户端
            if (targetChannel instanceof SocketChannel
                    && targetChannel != sourceChannel) {
                try {
                    // 将信息发送到targetChannel客户端
                    ((SocketChannel) targetChannel).write(
                            StandardCharsets.UTF_8.encode(request));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
