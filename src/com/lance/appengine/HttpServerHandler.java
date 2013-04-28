package com.lance.appengine;

import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;

public class HttpServerHandler extends IdleStateAwareChannelHandler {
	boolean readingChunks;

	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!readingChunks) {
			HttpRequest request = (HttpRequest) e.getMessage();
			if ("/xpp2".equalsIgnoreCase(request.getUri()) && request.getMethod() == HttpMethod.GET) {
				HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				response.addHeader("connection", "close");
				ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
				return;
			}
			if (request.getMethod() == HttpMethod.CONNECT) {
				String[] uri = request.getUri().split(":");
				ctx.getPipeline().addFirst("ssl", ProxyPipelineFactory.SERVER_FACTORY.getStartTlsSSLHandler(uri[0], uri.length > 1 ? uri[1] : "443"));
				HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				ctx.getChannel().write(response);
				return;
			}
			readingChunks = request.isChunked();
			HttpClient.getHttpClient().run(request, ctx.getChannel());
		} else {
			HttpChunk chunk = (HttpChunk) e.getMessage();
			readingChunks = !chunk.isLast();
			HttpClient.getHttpClient().run(chunk, ctx.getChannel());
		}
	}

	@Override
	public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
		if (e.getState() == IdleState.ALL_IDLE) {
			e.getChannel().close();
			System.out.println("close idle channel.");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		System.out.println("Response Error," + e.getCause());
	}
}
