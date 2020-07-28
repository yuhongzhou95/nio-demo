package com.shadow.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NioClient {
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(13, 13, 3L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1024));

    public static void main(String[] args) throws IOException {
//        new NioClient().start("nickname");
    }

    public void start(String nickname) throws IOException {
        /**
         * 连接服务器端
         */
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8888));

        /**
         * 接收服务器端响应
         */
        // 新开线程，专门负责来接收服务器端的响应数据
        // selector ， socketChannel ， 注册
        Selector selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        threadPoolExecutor.execute(new NioClientHandler(selector));

        /**
         * 向服务器端发送数据
         */
        for (Scanner sc = new Scanner(System.in); sc.hasNextLine(); ) {
            String requestMsg = sc.nextLine();
            if (requestMsg != null && requestMsg.length() != 0) {
                socketChannel.write(StandardCharsets.UTF_8.encode(nickname + ":" + requestMsg));
            }
        }


    }
}
