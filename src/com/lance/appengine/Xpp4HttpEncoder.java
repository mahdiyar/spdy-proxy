package com.lance.appengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
public class Xpp4HttpEncoder extends OneToOneEncoder {
	private final static Map<String, Boolean> ignoreHeaders = new HashMap<String, Boolean>();

	static {
		try {
			InputStream input = Xpp4HttpEncoder.class.getResourceAsStream("header.properties");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			String header;
			while ((header = reader.readLine()) != null) {
				ignoreHeaders.put(header.toLowerCase(), true);
			}
			reader.close();
		} catch (IOException e) {
			System.out.println("read headers.ini failed");
		}
	}

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof HttpRequest) {
			// Prepare the HTTP request.
			HttpRequest rawRequest = (HttpRequest) msg;
			String host = "go2application.appspot.com";
			DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, String.format("https://%s/xpp4", host));
			request.setHeader(HttpHeaders.Names.HOST, host);
			request.setHeader("X-SPDY-Stream-ID", rawRequest.getHeader("X-SPDY-Stream-ID"));
			request.setHeader("X-SPDY-Stream-Priority", 0);
			request.setChunked(rawRequest.isChunked());

			request.setHeader("m", rawRequest.getMethod().toString());

			if (!rawRequest.getUri().startsWith("http://") && !rawRequest.getUri().startsWith("https://")) {
				request.setHeader("p", String.format("https://%s%s", rawRequest.getHeader("host"), rawRequest.getUri()));
			} else {
				request.setHeader("p", rawRequest.getUri());
			}

			for (Entry<String, String> h : rawRequest.getHeaders()) {
				if (ignoreHeaders.containsKey(h.getKey().toLowerCase()))
					continue;
				request.addHeader(h.getKey(), h.getValue());
			}

			request.setHeader("content-length", rawRequest.getContent().readableBytes());
			request.setContent(rawRequest.getContent());

			return request;
		}

		System.out.println("chunk message");
		return msg;
	}
}
