package common;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client implements Closeable {
    private final SocketChannel channel;

    public Client() throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(InetAddress.getLoopbackAddress(), Constants.PORT));
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public void writeBlocking(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining())
            channel.write(buffer);
    }

    public void readBlocking(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer);
            if (read == -1)
                throw new EOFException("No more data in channel " + channel);
        }
    }
}
