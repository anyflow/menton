package net.anyflow.menton.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class DefaultWebSocketHandshaker implements WebSocketHandshaker {

	public DefaultWebSocketHandshaker() {

	}

	@Override
	public WebSocketServerHandshaker handshake(ChannelHandlerContext ctx, FullHttpRequest request) {

		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://" + request.headers().get(HOST) + "/websocket", null,
				false);
		WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(request);

		if(handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
			return null;
		}
		else {
			handshaker.handshake(ctx.channel(), request);
			return handshaker;
		}
	}
}