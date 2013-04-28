package com.lance.appengine;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.npn.NextProtoNego;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpServerCodec;
import org.jboss.netty.handler.codec.spdy.SpdyFrameCodec;
import org.jboss.netty.handler.codec.spdy.SpdyHttpCodec;
import org.jboss.netty.handler.codec.spdy.SpdySessionHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

public class ProxyPipelineFactory implements ChannelPipelineFactory {

	private SSLContext sslContext;
	private Boolean server;
	private Timer timer;
	public final static ProxyPipelineFactory CLIENT_FACTORY = new ProxyPipelineFactory(false);
	public final static ProxyPipelineFactory SERVER_FACTORY = new ProxyPipelineFactory(true);

	private ProxyPipelineFactory(boolean server) {
		this.server = server;
		if (!server) {
			try {
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, null, new java.security.SecureRandom());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				timer = new HashedWheelTimer();
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(new KeyManager[] { new CustomKeyManager() }, TrustManagerFactory.getTrustManagers(), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	public ChannelPipeline getPipeline() throws Exception {
		if (server)
			return getServerPipeline();
		return getClientPipeline();

	}

	public SslHandler getStartTlsSSLHandler(String uri, String port) {
		SSLEngine engine = sslContext.createSSLEngine(uri, Integer.parseInt(port));
		engine.setUseClientMode(false);
		return new SslHandler(engine, true);
	}

	public ChannelPipeline getClientPipeline() throws Exception {
		System.out.println("get client pipeline");
		ChannelPipeline pipeline = Channels.pipeline();

		SSLEngine engine = sslContext.createSSLEngine();
		engine.setUseClientMode(true);

		NextProtoNego.put(engine, new SimpleClientProvider());

		pipeline.addLast("ssl", new SslHandler(engine));
		pipeline.addLast("spdy_codec", new SpdyFrameCodec(3));
		pipeline.addLast("spdy_session_handler", new SpdySessionHandler(3, false));
		pipeline.addLast("spdy_http_codec", new SpdyHttpCodec(3, 32 * 1024 * 1024));
		pipeline.addLast("xpp4encoder", new Xpp4HttpEncoder());
		pipeline.addLast("xpp4decoder", new Xpp4HttpDecoder());
		pipeline.addLast("handler", new HttpClientHandler());

		return pipeline;
	}

	public ChannelPipeline getServerPipeline() throws Exception {

		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("http_codec", new HttpServerCodec(4096, 8192, 32 * 1024 * 1024));
		pipeline.addFirst("idle", new IdleStateHandler(timer, 0, 0, 120));
		pipeline.addLast("handler", new HttpServerHandler());

		return pipeline;
	}
}
