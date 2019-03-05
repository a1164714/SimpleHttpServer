package niomultiacceptors;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Reactor implements Runnable {
    final ServerSocketChannel serverSocket;
    final Selector[] selectors = new Selector[4];
    int next = 0;

    public Reactor(int port) throws IOException {
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress("localhost", port));
        serverSocket.configureBlocking(false);
        for (int i = 0; i < selectors.length; i++) {
            selectors[i] = Selector.open();
            SelectionKey sk = serverSocket.register(selectors[i], SelectionKey.OP_ACCEPT);
            sk.attach(new Acceptor());
        }
    }

    void dispatch(SelectionKey k) {
        Runnable r = (Runnable) (k.attachment());
        if (r != null) r.run();
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                for (int i = 0; i < selectors.length; i++) {
                    selectors[i].select();
                    Set<SelectionKey> selected = selectors[i].selectedKeys();
                    Iterator<SelectionKey> it = selected.iterator();
                    while (it.hasNext())
                        dispatch(it.next());
                    selected.clear();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class Acceptor implements Runnable {
        public synchronized void run() {
            try {
                SocketChannel c = serverSocket.accept();
                if (c != null) new Handler(selectors[next], c);
                if (++next == selectors.length) next = 0;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            new Thread(new Reactor(8080)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
