package io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class Server implements Runnable {
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("localhost", 8080));
            while (!Thread.interrupted())
                new Thread(new Handler(serverSocket.accept())).start(); // 此处阻塞等待accept
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
