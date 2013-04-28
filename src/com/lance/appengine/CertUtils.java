package com.lance.appengine;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

@SuppressWarnings("deprecation")
public class CertUtils {

	public static void createAcIssuerCert() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024, new SecureRandom());
		KeyPair keypair = keyPairGenerator.genKeyPair();

		X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

		// signers name
		String issuer = "CN=JPA CA, OU=JPA CA, O=JPA, L=JPA, ST=JPA, C=CN";
		// subjects name - the same as we are self signed.
		String subject = issuer;
		// create the certificate - version 3
		v3CertGen.setSerialNumber(BigInteger.valueOf(0x1234ABCDL));
		v3CertGen.setIssuerDN(new X509Principal(issuer));
		v3CertGen.setNotBefore(new Date(2013 - 1900, 0, 1));
		v3CertGen.setNotAfter(new Date(2030 - 1900, 0, 1));
		v3CertGen.setSubjectDN(new X509Principal(subject));
		v3CertGen.setPublicKey(keypair.getPublic());
		v3CertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

		// Is a CA
		v3CertGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(true));

		v3CertGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(keypair.getPublic()));

		X509Certificate cert = v3CertGen.generateX509Certificate(keypair.getPrivate());

		cert.checkValidity(new Date());

		cert.verify(keypair.getPublic());

		PEMWriter pemWriter = new PEMWriter(new FileWriter("e:\\pem\\ca.crt"));
		pemWriter.writeObject(cert);
		pemWriter.writeObject(keypair);
		pemWriter.close();
	}

	public static void createClientCert(String host) throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024, new SecureRandom());
		KeyPair keypair = keyPairGenerator.genKeyPair();

		PEMReader pemReader = new PEMReader(new FileReader("e:\\pem\\ca.crt"));
		pemReader.readObject();
		KeyPair caKey = (KeyPair) pemReader.readObject();
		pemReader.close();

		X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

		// issuer
		String issuer = "CN=JPA CA, OU=JPA CA, O=JPA, L=JPA, ST=JPA, C=CN";

		// subjects name table.
		Hashtable<DERObjectIdentifier, String> attrs = new Hashtable<DERObjectIdentifier, String>();
		Vector<DERObjectIdentifier> order = new Vector<DERObjectIdentifier>();

		attrs.put(X509Principal.C, "CN");
		attrs.put(X509Principal.O, "JPA");
		attrs.put(X509Principal.OU, "JPA");
		attrs.put(X509Principal.CN, host);

		order.addElement(X509Principal.C);
		order.addElement(X509Principal.O);
		order.addElement(X509Principal.OU);
		order.addElement(X509Principal.CN);

		// create the certificate - version 3
		v3CertGen.reset();

		v3CertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
		v3CertGen.setIssuerDN(new X509Principal(issuer));
		v3CertGen.setNotBefore(new Date(2013 - 1900, 0, 1));
		v3CertGen.setNotAfter(new Date(2030 - 1900, 0, 1));
		v3CertGen.setSubjectDN(new X509Principal(order, attrs));
		v3CertGen.setPublicKey(keypair.getPublic());
		v3CertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

		X509Certificate cert = v3CertGen.generateX509Certificate(caKey.getPrivate());

		cert.checkValidity(new Date());

		cert.verify(caKey.getPublic());

		PEMWriter pemWriter = new PEMWriter(new FileWriter(String.format("e:\\pem\\%s.crt", host)));
		pemWriter.writeObject(cert);
		pemWriter.writeObject(keypair);
		pemWriter.close();
	}
}
