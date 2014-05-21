package net.anyflow.menton.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;


public interface WebSocketHandshaker {
	WebSocketServerHandshaker handshake(ChannelHandlerContext ctx, FullHttpRequest request);
}