package net.anyflow.menton.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;

public abstract class WebsocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WebsocketFrameHandler.class);

	public static final String NAME = "WebsocketFrameRouter";

	private WebSocketServerHandshaker handshaker = null;

	protected abstract void websocketFrameReceived(ChannelHandlerContext ctx, WebSocketFrame wsframe);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame wsframe) throws Exception {
		if (wsframe instanceof CloseWebSocketFrame) {
			if (handshaker == null) {
				logger.error("handshaker cannot be null");
				return;
			}

			handshaker.close(ctx.channel(), (CloseWebSocketFrame) wsframe.retain());
			logger.debug("CloseWebSocketFrame handled.");
			return;
		}
		if (wsframe instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(wsframe.content().retain()));
			logger.debug("PingWebSocketFrame handled.");
			return;
		}

		websocketFrameReceived(ctx, wsframe);
	}

	public void setWebsocketHandshaker(WebSocketServerHandshaker handshaker) {
		this.handshaker = handshaker;
	}
}