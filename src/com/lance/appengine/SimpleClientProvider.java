package com.lance.appengine;
import java.util.List;

import org.eclipse.jetty.npn.NextProtoNego.ClientProvider;

public class SimpleClientProvider implements ClientProvider {
	String protocolSelected;
	
	@Override
	public boolean supports()
    {
        return true;
    }

	@Override
    public void unsupported()
    {
    }

	@Override
    public String selectProtocol(List<String> protocols)
    {
		for (String protocol : protocols) {
			if("spdy/3".equalsIgnoreCase(protocol)){
				protocolSelected= "spdy/3";
				break;
			}
		}
		
		if(protocolSelected==null)
			protocolSelected="http";
		
        return protocolSelected;
    }

	public String getSelectedProtocol() {
		return protocolSelected;
	}
}
