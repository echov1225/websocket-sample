package com.imooc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Business core Class that receive/processes/responds to the client's WebSocket request
 */
public class MyChannelHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker webSocketServerHandshaker;

    private static final String WEB_SOCKET_URL= "ws://localhost:8888/websocket";

    /**
     * The core method for the server to handle client-side WebSocket requests
     */
    @Override
    protected void messageReceived(ChannelHandlerContext context, Object msg) throws Exception {
        // 处理客户端向服务端发起http握手请求的业务
        if(msg instanceof FullHttpRequest) {
            handHttpRequest(context, (FullHttpRequest) msg);
        }
        // 处理websocket连接业务
        else if(msg instanceof WebSocketFrame) {
            handWebSocketFrame(context, (WebSocketFrame) msg);
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 创建连接
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.add(ctx.channel());
        System.out.println("连接开启...");
    }

    /**
     * 断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.remove(ctx.channel());
        System.out.println("连接关闭...");
    }

    /**
     * 完成接收
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 处理客户端向服务器发起http握手请求的业务
     */
    private void handHttpRequest(ChannelHandlerContext context, FullHttpRequest request) {
        if(!request.getDecoderResult().isSuccess() || !("websocket".equals(request.headers().get("Upgrade")))) {
            sendHttpResponse(context, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, false);
        webSocketServerHandshaker = wsFactory.newHandshaker(request);
        if(webSocketServerHandshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(context.channel());
        } else {
            webSocketServerHandshaker.handshake(context.channel(), request);
        }
    }

    /**
     * 服务端向客户端响应消息
     */
    private void sendHttpResponse(ChannelHandlerContext context, FullHttpRequest request, DefaultFullHttpResponse response) {
        if(response.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(response.getStatus().toString(), StandardCharsets.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
        }
        // 服务端向客户端发送数据
        ChannelFuture future = context.channel().writeAndFlush(response);
        if(response.getStatus().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 处理客户端与服务端之间的websocket业务
     */
    private void handWebSocketFrame(ChannelHandlerContext context, WebSocketFrame frame) {
        // 判断是否是关闭websocket指令
        if(frame instanceof CloseWebSocketFrame) {
            webSocketServerHandshaker.close(context.channel(), (CloseWebSocketFrame) frame.retain());
        }
        // 判断是否是ping消息
        if(frame instanceof PingWebSocketFrame) {
            context.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 判断是否是二进制消息，如果是二进制消息，抛出异常
        if(!(frame instanceof  TextWebSocketFrame)) {
            System.out.println("Binary message are not currently support");
            throw new IllegalArgumentException(" [" + this.getClass().getName() + "] Not support message");
        }

        // 返回应答消息
        // 获取客户端向服务端发送的信息
        String request = ((TextWebSocketFrame) frame).text();
        TextWebSocketFrame twsf = new TextWebSocketFrame(new Date().toString() + " [" + context.channel() + "] ===>>> " +request);
        // 群发消息
        NettyConfig.group.writeAndFlush(twsf);
    }
}
