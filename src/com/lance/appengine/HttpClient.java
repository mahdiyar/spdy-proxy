package com.lance.appengine;

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;

import org.eclipse.jetty.npn.NextProtoNego;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * A simple HTTP client that prints out the content of the HTTP response to
 * {@link System#out} to test {@link HttpSnoopServer}.
 */
public class HttpClient {
	private static HttpClient client = new HttpClient();
	private volatile Channel channelToGAE;
	private final ClientBootstrap bootstrap;
	private int currentStreamId = 1;
	private final HashMap<Integer, Channel> channels = new HashMap<Integer, Channel>();
	private final LinkedList<Object> pendingRequests = new LinkedList<Object>();
	private final Thread requestExecuteThread;
	private final Object connectionLock = new Object();

	protected HttpClient() {
		NextProtoNego.debug = true;
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

		bootstrap.setPipelineFactory(ProxyPipelineFactory.CLIENT_FACTORY);

		requestExecuteThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					executeRequest();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, "request-dispatch-thread");
		requestExecuteThread.start();
	}

	public static HttpClient getHttpClient() {
		return client;
	}

	public void reconnect() {
		System.out.println("begin reconnect proc...");
		channelToGAE = null;
	}

	public void Close() {
		channelToGAE.getCloseFuture().awaitUninterruptibly();

		bootstrap.releaseExternalResources();
	}

	public void run(Object message, Channel channel) {
		if (message instanceof HttpRequest) {
			HttpRequest rawRequest = (HttpRequest) message;
			int streamId = getNextStreamId();
			rawRequest.setHeader("X-SPDY-Stream-ID", streamId);
			channels.put(streamId, channel);

			addRequestToQueue(rawRequest);
		} else if (message instanceof HttpChunk) {
			System.out.println("chunk message");
			addRequestToQueue(message);
		}
	}

	private synchronized void addRequestToQueue(Object req) {
		pendingRequests.addFirst(req);
		notify();
	}

	private synchronized Object getRequestToQueue() throws InterruptedException {
		if (pendingRequests.isEmpty())
			wait();
		return pendingRequests.removeLast();
	}

	private void executeRequest() throws InterruptedException {
		while (true) {
			final Object req = getRequestToQueue();
			Channel ch = channelToGAE;
			if (ch != null) {
				ch.write(req);
			} else {
				System.out.println("connecting....");
				ChannelFuture future = bootstrap.connect(new InetSocketAddress("www.google.com.hk", 443));
				// Wait until the connection attempt succeeds or fails.
				future.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (future.isSuccess()) {
							channelToGAE = future.getChannel();
							channelToGAE.getCloseFuture().addListener(new ChannelFutureListener() {
								@Override
								public void operationComplete(ChannelFuture future) throws Exception {
									System.out.println("connection closed");
									reconnect();
								}
							});
							if (channelToGAE.isConnected()) {
								System.out.println("connect success");
								channelToGAE.write(req);
							}
							synchronized (connectionLock) {
								connectionLock.notify();
							}
						} else {
							System.out.println("connect failed");
							synchronized (connectionLock) {
								connectionLock.notify();
							}
							addRequestToQueue(req);
						}
					}
				});
				if (channelToGAE == null) {
					synchronized (connectionLock) {
						connectionLock.wait();
					}
				}
			}
		}
	}

	public void response(int streamId, Object response, boolean lastMessage) {
		try {
			channels.get(streamId).write(response);
		} finally {
			if (lastMessage)
				channels.remove(streamId);
		}
	}

	private synchronized int getNextStreamId() {
		int result = currentStreamId;
		currentStreamId += 2;
		return result;
	}
}
