/**
 * 
 */
package net.anyflow.menton.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anyflow
 */
public class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

	static final Logger logger = LoggerFactory.getLogger(HttpClientHandler.class);

	private final MessageReceiver receiver;
	private final HttpRequest request;
	private HttpResponse response;

	public HttpClientHandler(MessageReceiver receiver, HttpRequest request) {
		this.receiver = receiver;
		this.request = request;
	}

	public HttpResponse httpResponse() {
		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty .channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
		if(ctx.channel().isActive() == false) { return; }

		response = HttpResponse.createFrom(msg, ctx.channel());

		HttpClient.logger.debug("[response] STATUS : " + response.getStatus());
		HttpClient.logger.debug("[response] VERSION : " + response.getProtocolVersion());

		if(!response.headers().isEmpty()) {
			for(String name : response.headers().names()) {
				for(String value : response.headers().getAll(name)) {
					HttpClient.logger.debug("[response] HEADER : " + name + " = " + value);
				}
			}
		}

		if(response.content().isReadable()) {
			HttpClient.logger.debug("[response] CONTENT {");
			HttpClient.logger.debug(response.content().toString(CharsetUtil.UTF_8));
			HttpClient.logger.debug("[response] } END OF CONTENT");
		}

		if(receiver != null) {
			request.setChannel(ctx.channel());
			receiver.messageReceived(request, response);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);
		ctx.channel().close();
	}
}