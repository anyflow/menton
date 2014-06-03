package net.anyflow.menton.http;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;

public interface WebSocketFrameHandler {
	void handle(WebSocketServerHandshaker handshaker, ChannelHandlerContext ctx, WebSocketFrame frame);
	List<String> subprotocols();
}