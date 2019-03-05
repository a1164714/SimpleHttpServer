package niomulti;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.*;

final class Handler implements Runnable {
    final SocketChannel socket;
    final SelectionKey sk;
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    static ThreadPoolExecutor pool = new ThreadPoolExecutor(
            POOL_SIZE,
            POOL_SIZE * 2 + 1,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

    ByteBuffer input = ByteBuffer.allocate(1024 * 8);
    ByteBuffer output = ByteBuffer.allocate(1024 * 8);
    static final int READING = 0, SENDING = 1, PROCESSING = 3;
    int state = READING;

    Handler(Selector sel, SocketChannel c)
            throws IOException {
        socket = c;
        c.configureBlocking(false);
        sk = socket.register(sel, 0);
        sk.attach(this);
        sk.interestOps(SelectionKey.OP_READ);
        sel.wakeup();
    }

    boolean inputIsComplete() {
        return true;
    }

    boolean outputIsComplete() {
        return true;
    }

    @Override
    public void run() {
        try {
            if (state == READING) read();
            else if (state == SENDING) send();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

     void read() throws IOException {
        socket.read(input);
        if (inputIsComplete()) {
            state = PROCESSING;
            pool.execute(new Processer());
        }
    }

     void send() throws IOException {
        output.flip();
        socket.write(output);
        if (outputIsComplete()) sk.cancel();
    }

    private class Processer implements Runnable {
        void process() {
            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder decoder = charset.newDecoder();
            try {
                input.flip();
                CharBuffer charBuffer = decoder.decode(input.asReadOnlyBuffer());
                System.out.println(charBuffer.toString());
            } catch (CharacterCodingException e) {
                e.printStackTrace();
            }
            String result = new String("HTTP/1.1 200 OK\n" +
                    "Content-Type: text/html;charset=UTF-8\n" +
                    "Content-Length: 11\n\n" +
                    "hello world");
            output.put(result.getBytes());
        }

        @Override
        public void run() {
            process();
            state = SENDING;
            sk.interestOps(SelectionKey.OP_WRITE);
            sk.selector().wakeup();
        }
    }

}
