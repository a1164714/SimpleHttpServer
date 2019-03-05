package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Reactor implements Runnable {
    final Selector selector;
    final ServerSocketChannel serverSocket;
    final Acceptor acceptor;

    public Reactor(int port) throws IOException {
        acceptor = new Acceptor();
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress("localhost", port));
        serverSocket.configureBlocking(false);
        SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        sk.attach(acceptor);
    }

    void dispatch(SelectionKey k) {
        Runnable r = (Runnable) (k.attachment());
        if (r != null) r.run();
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    dispatch(it.next());
                    it.remove();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class Acceptor implements  Runnable{

        public void run() {
            try {
                SocketChannel channel = serverSocket.accept();
                if (channel != null) new Handler(selector, channel);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 启动服务器
     */
    public void start() {
        new Thread(this).start();
    }

    public static void main(String[] args) {
        try {
            new Reactor(8080).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
