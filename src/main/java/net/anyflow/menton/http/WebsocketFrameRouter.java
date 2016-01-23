package net.anyflow.menton.http;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebsocketFrameRouter extends MessageToMessageDecoder<WebSocketFrame> {

	final WebsocketFrameHandler wsfHandler;

	public WebsocketFrameRouter(WebsocketFrameHandler wsfHandler) {
		this.wsfHandler = wsfHandler;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {
		wsfHandler.websocketFrameReceived(ctx, msg);
	}
}