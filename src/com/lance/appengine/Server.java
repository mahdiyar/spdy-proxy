package com.lance.appengine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Security;
import java.util.concurrent.Executors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class Server {
	public static void main(String[] args) throws IOException {
		Security.addProvider(new BouncyCastleProvider());
		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap((ChannelFactory) new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(ProxyPipelineFactory.SERVER_FACTORY);

		// Bind and start to accept incoming connections.
		int port=9999;
		
		if(args.length>0)
			port= Integer.parseInt(args[0]);
		
		bootstrap.bind(new InetSocketAddress(port));
		
		System.out.println("spdy started.");

		
		System.in.read();
	}
}
