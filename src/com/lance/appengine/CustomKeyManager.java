package com.lance.appengine;
import java.io.File;
import java.io.FileReader;
import java.net.Socket;
import java.security.KeyPair;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

import javax.net.ssl.SSLEngine;

import org.bouncycastle.openssl.PEMReader;

public final class CustomKeyManager extends javax.net.ssl.X509ExtendedKeyManager {
	private final static String CERT_PATH = "e:\\pem\\";
	private X509Certificate caCert;
	private Hashtable<String, X509Certificate> serverCerts = new Hashtable<String, X509Certificate>();
	private Hashtable<String, KeyPair> serverKeys = new Hashtable<String, KeyPair>();

	CustomKeyManager() throws Exception {
		File caFile = new File(CERT_PATH + "ca.crt");
		if (!caFile.exists()) {
			CertUtils.createAcIssuerCert();
		}

		PEMReader pemReader = new PEMReader(new FileReader(caFile));
		this.caCert = (X509Certificate) pemReader.readObject();
		pemReader.close();
	}

	@Override
	public String[] getClientAliases(String string, Principal[] prncpls) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String chooseClientAlias(String[] strings, Principal[] prncpls, Socket socket) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
		return engine.getPeerHost();
	}
	
	@Override
	public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
		return engine.getPeerHost();
	}
	
	@Override
	public String[] getServerAliases(String string, Principal[] prncpls) {
		return (new String[] { string });
	}

	@Override
	public String chooseServerAlias(String string, Principal[] prncpls, Socket socket) {
		return string;
	}

	@Override
	public X509Certificate[] getCertificateChain(String string) {
		X509Certificate x509certificates[] = new X509Certificate[2];

		x509certificates[0] = serverCerts.get(string);
		x509certificates[1] = caCert;

		return x509certificates;
	}

	@Override
	public PrivateKey getPrivateKey(String string) {
		if (!serverCerts.containsKey(string)) {
			File certFile = new File(CERT_PATH + string + ".crt");
			try {
				if (!certFile.exists()) {
					CertUtils.createClientCert(string);
				}
				PEMReader pemReader = new PEMReader(new FileReader(certFile));
				serverCerts.put(string, (X509Certificate) pemReader.readObject());
				serverKeys.put(string, (KeyPair) pemReader.readObject());
				pemReader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return this.serverKeys.get(string).getPrivate();
	}
}