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

import java.util.Random;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * A simple HTTP client that prints out the content of the HTTP response to
 * {@link System#out} to test {@link HttpSnoopServer}.
 */
public class Xpp2HttpEncoder extends OneToOneEncoder {
	private final Random random = new Random();

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof HttpRequest) {
			// Prepare the HTTP request.
			HttpRequest rawRequest = (HttpRequest) msg;
			String host = random.nextInt() % 2 == 0 ? "java2theworld.appspot.com" : "java2theworld0.appspot.com";
			DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, String.format("https://%s/xpp2", host));
			request.setHeader(HttpHeaders.Names.HOST, host);
			request.setHeader("X-SPDY-Stream-ID", rawRequest.getHeader("X-SPDY-Stream-ID"));
			request.setHeader("X-SPDY-Stream-Priority", 0);
			request.setChunked(rawRequest.isChunked());
			request.setContent(rawRequest.getContent());

			return request;
		}
		System.out.println("chunk message");
		return msg;
	}
}
