package nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

final class Handler2 implements Runnable {
    final SocketChannel socket;
    final SelectionKey sk;
    ByteBuffer input = ByteBuffer.allocate(1024 * 8);
    ByteBuffer output = ByteBuffer.allocate(1024 * 8);

    Handler2(Selector sel, SocketChannel channel)
            throws IOException {
        channel.configureBlocking(false);
        socket = channel;
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
        try {
            socket.read(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inputIsComplete()) {
            process();
            sk.attach(new Sender());
            sk.interestOps(SelectionKey.OP_WRITE);
            sk.selector().wakeup();
        }
    }

    /**
     * Gof: State Object模式
     */
    class Sender implements Runnable {
        public void run(){
            try {
                output.flip();
                socket.write(output);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (outputIsComplete()) sk.cancel();
        }
    }


}
