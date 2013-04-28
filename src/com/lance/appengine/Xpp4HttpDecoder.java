package com.lance.appengine;

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
/**
 * A simple HTTP client that prints out the content of the HTTP response to
 * {@link System#out} to test {@link HttpSnoopServer}.
 */
public class Xpp4HttpDecoder extends OneToOneDecoder {

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof HttpResponse) {
			HttpResponse rawResponse = (HttpResponse) msg;
			if (rawResponse.containsHeader("c")) {
				rawResponse.setHeader("content-encoding", rawResponse.getHeader("c"));
				rawResponse.removeHeader("c");
			}
			ProcessCooike(rawResponse);
			return rawResponse;
		}

		System.out.println("chunk message");
		return msg;
	}

	protected void ProcessCooike(HttpResponse response) {
		String setCookies = response.getHeader("set-cookie");
		if (setCookies != null) {
			String[] strArray = setCookies.split(", ");
			List<String> list = new ArrayList<String>();
			for (String str : strArray) {
				int index = str.indexOf("=");
				int num2 = str.indexOf(";");
				if ((index < 0) || ((num2 > -1) && (index > num2))) {
					list.set(list.size() - 1, String.format("%s, %s", list.get(list.size() - 1), str));
				} else {
					list.add(str);
				}
			}
			response.removeHeader("set-cookie");
			for (String str2 : list) {
				response.addHeader("set-cookie", str2);
			}
		}
	}

}
