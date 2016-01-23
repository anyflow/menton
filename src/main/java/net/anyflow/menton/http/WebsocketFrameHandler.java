package net.anyflow.menton.http;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public interface WebsocketFrameHandler {

	public static final List<String> DEFAULT_SUBPROTOCOLS = null;
	public static final boolean ALLOW_EXTENSIONS = false;
	public static final int MAX_FRAME_SIZE = 65536;

	public List<String> subprotocols();

	public String websocketPath();

	public boolean allowExtensions();

	public int maxFrameSize();

	public void websocketFrameReceived(ChannelHandlerContext ctx, WebSocketFrame wsframe);
}