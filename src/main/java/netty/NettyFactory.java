package netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;

public abstract class NettyFactory {

    public static NettyFactory getDefault() {
        if(IOUring.isAvailable())
            return new IOUringFactory();
        return Epoll.isAvailable() ? new EpollFactory() : new NioFactory();
    }

    public abstract Class<? extends ServerSocketChannel> getServerSocketChannel();

    public EventLoopGroup createEventLoopGroup() {
        return createEventLoopGroup(0);
    }

    public abstract EventLoopGroup createEventLoopGroup(int nThreads);

    private static class NioFactory extends NettyFactory {
        @Override
        public EventLoopGroup createEventLoopGroup(int nThreads) {
            return new NioEventLoopGroup(nThreads);
        }

        @Override
        public Class<NioServerSocketChannel> getServerSocketChannel() {
            return NioServerSocketChannel.class;
        }
    }

    private static class EpollFactory extends NettyFactory {
        @Override
        public EventLoopGroup createEventLoopGroup(int nThreads) {
            return new EpollEventLoopGroup(nThreads);
        }

        @Override
        public Class<? extends ServerSocketChannel> getServerSocketChannel() {
            return EpollServerSocketChannel.class;
        }
    }

    private static class IOUringFactory extends NettyFactory {
        @Override
        public EventLoopGroup createEventLoopGroup(int nThreads) {
            return new IOUringEventLoopGroup(nThreads);
        }

        @Override
        public Class<? extends ServerSocketChannel> getServerSocketChannel() {
            return IOUringServerSocketChannel.class;
        }
    }
}
