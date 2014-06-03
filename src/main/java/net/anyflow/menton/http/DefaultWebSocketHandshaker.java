package net.anyflow.menton.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class DefaultWebSocketHandshaker implements WebSocketHandshaker {

	private final List<String> subprotocols;
	
	public DefaultWebSocketHandshaker(List<String> subprotocols) {
		this.subprotocols = subprotocols;
	}
	
	private String toCsv(List<String> list) {
		if(list == null || list.size() == 0) { return null; }
		
		StringBuilder sb = new StringBuilder();
		for(String item : subprotocols) {
			sb.append(item + ",");
		}
		String ret = sb.toString();
		
		if(ret.endsWith(",")) { ret = ret.substring(0, ret.length() - 1); }
		
		return ret;
	}
	
	@Override
	public WebSocketServerHandshaker handshake(ChannelHandlerContext ctx, FullHttpRequest request) {
		
		WebSocketServerHandshakerFactory wsFactory 
			= new WebSocketServerHandshakerFactory("ws://" + request.headers().get(HOST) + "/websocket", toCsv(subprotocols), false);
		
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