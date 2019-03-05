package io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Handler implements Runnable {
    private Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            byte[] bs = new byte[1024 * 8];
            in.read(bs);
            System.out.print(new String(bs));
            System.out.println();
            String s = new String("HTTP/1.1 200 OK\n" +
                    "Content-Type: text/html;charset=UTF-8\n" +
                    "Content-Length: 11\n\n" +
                    "hello world");
            out.write(s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
