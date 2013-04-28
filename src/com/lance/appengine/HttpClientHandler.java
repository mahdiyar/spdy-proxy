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

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class HttpClientHandler extends SimpleChannelUpstreamHandler {

	private boolean readingChunks;
	private int streamId;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!readingChunks) {
			HttpResponse response = (HttpResponse) e.getMessage();
			if (response.getHeader("content-encoding1") != null) {
				response.addHeader("content-encoding", response.getHeader("content-encoding1"));
				response.removeHeader("content-encoding1");
			}
			if (response.isChunked())
				readingChunks = true;
			streamId = Integer.parseInt(response.getHeader("X-SPDY-Stream-ID"));
			HttpClient.getHttpClient().response(streamId, response, !readingChunks);
		} else {
			HttpChunk chunk = (HttpChunk) e.getMessage();
			readingChunks = !chunk.isLast();
			HttpClient.getHttpClient().response(streamId, chunk, chunk.isLast());
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		System.out.println("request error:" + e.getCause());
	}
}
