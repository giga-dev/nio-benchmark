package nio;

import com.gigaspaces.lrmi.nio.async.LRMIThreadPoolExecutor;
import common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static nio.Util.*;

public class ReadInSelectorServer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    Selector clientSelector;

    public void run( int port) throws IOException
    {
        final ExecutorService executor;
        if(poolType.equals("fixed"))
            executor = Executors.newFixedThreadPool( poolSize );
        else if(poolType.equals("work-stealing"))
            executor = Executors.newWorkStealingPool( poolSize );
        else if(poolType.equals("dynamic"))
            executor = new LRMIThreadPoolExecutor(0, poolSize, 60000, Integer.MAX_VALUE, Long.MAX_VALUE,
                    Thread.NORM_PRIORITY,
                    "LRMI-Custom",
                    true, true);
        else throw new IllegalArgumentException("");
        logger.info("pool size: {}, poolType: {}", poolSize, poolType);
        clientSelector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        InetSocketAddress sa =  new InetSocketAddress( InetAddress
                .getLoopbackAddress(), port );
        ssc.socket().bind( sa );
        ssc.register( clientSelector, SelectionKey.OP_ACCEPT );

        while ( true ) {
            try {
                while ( clientSelector.select(100) == 0 );
                Set<SelectionKey> readySet = clientSelector.selectedKeys();
                for(Iterator<SelectionKey> it=readySet.iterator();
                    it.hasNext();)
                {
                    final SelectionKey key = it.next();
                    it.remove();
                    if ( key.isAcceptable() ) {
                        acceptClient( ssc );
                    } else if(key.isReadable()){
//                        key.interestOps(0);
                        ChannelEntry entry = (ChannelEntry) key.attachment();
                        ByteBuffer buffer = ByteBuffer.allocate(Constants.MAX_PAYLOAD);
                        SocketChannel channel = entry.socketChannel;
                        try {
                            int data = channel.read(buffer);
                            if (data == -1 || buffer.get(buffer.position() - 1) == '\n') {
                                executor.submit(new ChannelEntryTask(key, entry, clientSelector, buffer));
                            } else {
                                logger.warn("failed to read from buffer. data = " + data);
                            }
                        } catch (IOException e) {
                            logger.warn("Failed to read from " + channel + " - cancelling key" + System.lineSeparator() + e);
                            key.cancel();
                        }
                    } else{
                        logger.warn("UNEXPECTED KEY");
                    }
                }
            } catch ( IOException e ) { logger.error("Failed to process selector", e); }
        }
    }

    void acceptClient( ServerSocketChannel ssc ) throws IOException
    {
        SocketChannel clientSocket = ssc.accept();
        clientSocket.configureBlocking(false);
        clientSocket.socket().setTcpNoDelay(true);
        clientSocket.register( clientSelector, SelectionKey.OP_READ, new ChannelEntry(clientSocket));
        logger.info("Added new client {}", clientSocket);
    }

    public static void main( String argv[] ) throws IOException {
       parseArgs(argv);
        new ReadInSelectorServer().run(Constants.PORT);
    }

    static class ChannelEntry{
        private final SocketChannel socketChannel;
        ChannelEntry(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }
        public void echo(ByteBuffer buff) {
            try {
                buff.flip();
                socketChannel.write(buff);
                if(buff.hasRemaining()) {
                    System.out.println("failed to write to buffer");
                }
                buff.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class ChannelEntryTask implements Runnable{
        private final ChannelEntry channelEntry;
        private final SelectionKey key;
        private final Selector selector;
        private final ByteBuffer buffer;
        ChannelEntryTask(SelectionKey key, ChannelEntry channelEntry, Selector selector, ByteBuffer buffer) {
            this.channelEntry = channelEntry;
            this.key = key;
            this.selector = selector;
            this.buffer = buffer;
        }
        @Override
        public void run() {
            channelEntry.echo(buffer);
//            key.interestOps(SelectionKey.OP_READ);
//            selector.wakeup();
        }
    }
}

