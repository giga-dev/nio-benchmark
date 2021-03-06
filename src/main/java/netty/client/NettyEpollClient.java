/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package netty.client;

import common.Settings;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.CharsetUtil;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
@State(Scope.Thread)
public class NettyEpollClient extends AbstractNettyClient{
    private final EpollEventLoopGroup group;

    public NettyEpollClient() {
        // Configure the client.
        group = new EpollEventLoopGroup(1);
        Bootstrap b = new Bootstrap();
        NettyEpollClient client = this;
        b.group(group)
                .channel(EpollSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        //p.addLast(new LoggingHandler(LogLevel.INFO));
                        p.addLast(new EchoClientHandler(client));
                    }
                });

        // Start the client.
        try {
            f = b.connect(Settings.ADDRESS).sync();
        }catch (Exception e){

        }

    }

    public CompletableFuture<String> sendMessageInternal(String message){
        byte[] bytes = message.getBytes(CharsetUtil.UTF_8);
        this.completableFuture = new CompletableFuture();
        f.channel().writeAndFlush(Unpooled.wrappedBuffer(bytes));
        return completableFuture;
    }

    public void sendMessage(String message) {
        String response = null;
        try {
            response = sendMessageInternal(message).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
//        System.out.println("Client received: " + response);
    }

//    public static void main(String[] args) {
//        ExecutorService executorService = Executors.newFixedThreadPool(4);
//
//        for (int i = 0; i < 4; i++) {
//            int finalI = i;
//            executorService.submit(() ->{
//                        EchoClient echoClient = new EchoClient();
//                echoClient.sendMessage("HELLO-" + finalI);
//            });
//        }
//    }

    public void close() throws Exception{
        try{
            f.channel().closeFuture().sync();
        }finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}

