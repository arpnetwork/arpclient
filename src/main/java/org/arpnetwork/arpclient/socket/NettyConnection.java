/*
 * Copyright 2018 ARP Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arpnetwork.arpclient.socket;

import org.arpnetwork.arpclient.data.ErrorInfo;
import org.arpnetwork.arpclient.data.Message;

import java.lang.ref.WeakReference;
import java.util.List;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.concurrent.GenericFutureListener;

public class NettyConnection {
    private static final int CONNECT_TIMEOUT = 10000;

    private ConnectionListener mListener;

    private EventLoopGroup mWorkerGroup;
    private ChannelFuture mChannelFuture;
    private GenericFutureListener<ChannelFuture> mChannelFutureListener;

    private boolean mClientDisconnected;

    public interface ConnectionListener {
        /**
         * Socket connected
         *
         * @param conn
         */
        void onConnected(NettyConnection conn);

        /**
         * Socket closed
         *
         * @param conn
         */
        void onClosed(NettyConnection conn);

        /**
         * Receive socket message
         *
         * @param conn
         * @param msg
         */
        void onMessage(NettyConnection conn, Message msg);

        /**
         * Socket error
         *
         * @param code See {@link ErrorInfo}
         * @param msg  Error details
         */
        void onError(int code, String msg);
    }

    public NettyConnection(ConnectionListener listener) {
        mListener = listener;
    }

    /**
     * Connect socket
     *
     * @param host socket ip
     * @param port socket port
     */
    public void connect(String host, int port) {
        mWorkerGroup = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.group(mWorkerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline()
                        .addLast("decoder", new MessageDecoder())
                        .addLast("encoder", new MessageEncoder())
                        .addLast(new ConnectionHandler(NettyConnection.this));
            }
        });

        mChannelFuture = b.connect(host, port);
        mChannelFutureListener = new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) {
                Throwable cause = future.cause();
                if (cause != null) {
                    mListener.onError(ErrorInfo.ERROR_NETWORK, cause.getMessage());
                }
            }
        };
        mChannelFuture.addListener(mChannelFutureListener);
    }

    /**
     * Close socket not for timeout reason
     */
    public void close() {
        close(false);
    }

    /**
     * Close socket
     *
     * @param timeout if for timeout reason
     */
    public void close(boolean timeout) {
        mClientDisconnected = !timeout;
        try {
            mChannelFuture.removeListener(mChannelFutureListener);
            mChannelFuture.sync().channel().close().sync();
            mWorkerGroup.shutdownGracefully();
        } catch (Exception e) {
        }
    }

    /**
     * Send socket message
     *
     * @param msg
     */
    public void write(Message msg) {
        if (!mChannelFuture.isSuccess()) {
            throw new IllegalStateException();
        }

        mChannelFuture.channel().writeAndFlush(msg);
    }

    private static class ConnectionHandler extends ChannelInboundHandlerAdapter {

        private WeakReference<NettyConnection> mConn;

        public ConnectionHandler(NettyConnection conn) {
            mConn = new WeakReference<>(conn);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            NettyConnection conn = mConn.get();
            if (conn != null) {
                conn.mListener.onConnected(conn);
            }

            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            NettyConnection conn = mConn.get();
            if (conn != null) {
                if (conn.mClientDisconnected) {
                    conn.mListener.onClosed(conn);
                } else {
                    conn.mListener.onError(ErrorInfo.ERROR_DISCONNECTED_BY_DEVICE, null);
                }
            }

            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            NettyConnection conn = mConn.get();
            if (conn != null) {
                conn.mListener.onMessage(conn, (Message) msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            NettyConnection conn = mConn.get();
            if (conn != null) {
                conn.mListener.onError(ErrorInfo.ERROR_NETWORK, cause.getMessage());
                conn.close();
            }
        }
    }

    private static class MessageDecoder extends ReplayingDecoder<Void> {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            out.add(Message.readFrom(in));
        }
    }

    private static class MessageEncoder extends MessageToByteEncoder<Message> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
            msg.writeTo(out);
        }
    }
}