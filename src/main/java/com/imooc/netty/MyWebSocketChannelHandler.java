package com.imooc.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Assign values to each components during initialization
 */
public class MyWebSocketChannelHandler extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline().addLast("http-codec", new HttpServerCodec());
        channel.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
        channel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
        channel.pipeline().addLast("handler", new MyChannelHandler());
    }
}
