import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

class SSLServer extends Server {
  private final String password = "password";

  /**
   * Constructor for SSLServer class with given port
   */
  public SSLServer(int serverPort) {
    super(serverPort);
    type = "SSL Server";
  }

  /**
   * Override Bind method from Server
   * @throws IOException
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableKeyException
   * @throws KeyManagementException
   * @throws CertificateException
   */
  public void bind() throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException, CertificateException {

    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(new FileInputStream("server.jks"), password.toCharArray());

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    keyManagerFactory.init(keyStore, password.toCharArray());

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

    SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
    serverSocket = sslServerSocketFactory.createServerSocket(this.serverPort);

    serverListenning = true;
    System.out.println("SSL Server bound and listening to port " + serverPort + ". Secure: yes" );
  }
}
