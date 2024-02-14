package eu.digitalsystems;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

/**
 * Hello world!
 */
public class EpollApp {

    private static final int BUFFER_SIZE = 1024;
    private static final int LISTENERS_COUNT = 2_000_000;

    // The buffer into which we'll read data when it's available
    private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private Selector selector;

    private long loopTime;
    private long numMessages = 0;

    public EpollApp() throws IOException {
        selector = initSelector();
    }

    public void listenLoop() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    // Check what event is available and deal with it
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.register(selector, SelectionKey.OP_READ);

        System.out.println("Client is connected");
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Clear out our read buffer so it's ready for new data
        readBuffer.clear();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(readBuffer);
        } catch (IOException e) {
            key.cancel();
            socketChannel.close();

            System.out.println("Forceful shutdown");
            return;
        }

        if (numRead == -1) {
            System.out.println("Graceful shutdown");
            key.channel().close();
            key.cancel();

            return;
        }

        socketChannel.register(selector, SelectionKey.OP_WRITE);

        numMessages++;
        if (numMessages % 100000 == 0) {
            long elapsed = System.currentTimeMillis() - loopTime;
            loopTime = System.currentTimeMillis();
            System.out.println(elapsed);
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer dummyResponse = ByteBuffer.wrap("ok".getBytes("UTF-8"));

        socketChannel.write(dummyResponse);
        if (dummyResponse.remaining() > 0) {
            System.err.print("Filled UP");
        }

        key.interestOps(SelectionKey.OP_READ);
    }

    private Selector initSelector() throws IOException {
        final Selector socketSelector = SelectorProvider.provider().openSelector();

        int numberOfListeners = 0;
        for (int address = 1; numberOfListeners < LISTENERS_COUNT; address++) {
            final InetAddress hostAddress = InetAddress.getByAddress(new byte[]{127, 0, 0, (byte) address});
            int lastPort = 0;
            for (int port = 2000; port < 32000 && numberOfListeners < LISTENERS_COUNT; port++, numberOfListeners++) {
                try {
                    final InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
                    final ServerSocketChannel serverChannel = ServerSocketChannel.open();
                    serverChannel.configureBlocking(false);
                    serverChannel.socket().bind(isa);
                    serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
                    if(port%1000 == 0) {
                        System.out.println("numberOfListeners till now: " + numberOfListeners);
                    }
                } catch (Exception ex) {
                    System.out.println("Exception occurred while creating listener number " + numberOfListeners + ", address: " + hostAddress + ", port: " + port + ".");
                    throw ex;
                }
                lastPort = port;
            }
            System.out.println(hostAddress + " -> listening from TCP port 2000, till " + lastPort + ".");
        }
        return socketSelector;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting echo server");
        new EpollApp();
    }
}
