import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;

public class Server implements Runnable {

  protected String type = "Non-secure Server";
  protected boolean serverListenning;
  protected int serverPort;
  protected Request req;
  private Socket clientSocket;
  private DataOutputStream toClientStream;
  private BufferedReader fromClientStream;
  protected ServerSocket serverSocket;

  /**
   * Constructor for Server Class with given port
   */
  public Server(int serverPort) {
    this.serverPort = serverPort;
  }

  /**
   * Start server
   */
  public void start() {
    try {
      bind();
      processRequests();
    }
    catch (Exception e) {
      serverListenning = false;
      debugm("Exception: " + e);
    }
  }

  //override
  public void run() {
    this.start();
  }

  /**
   * Creates a serverSocket and binds to the desired server-side port number.
   * @throws IOException if the port is already in use.
   */
  public void bind() throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException, CertificateException {
    serverSocket = new ServerSocket(serverPort);

    serverListenning = true;
    debugm("Server bound and listening to port " + serverPort + ". Secure: no");
  }

  /**
   * Accept connections and process HTTP requests once the server has been started.
   */
  private void processRequests() throws IOException {
    while (serverListenning) {
      setNewConnectionIfNeeded();
      boolean requestReceived = getRequestFromClient();
      if(requestReceived) {
        respondToRequest();
        closeCurrentConnectionIfPersistenceWasNotRequested();
      }
      else { //no request received. It means the client has closed the connection, then we close it too..
        clientSocket.close();
      }
    }
  }

  private void setNewConnectionIfNeeded() throws IOException {
    if(thereIsNoConnectionAlive()) {
      acceptConnectionFromClient();
    }
  }

  private boolean getRequestFromClient() throws IOException {
    debugm("Waiting for the first line of request string");
    String requestString = null;
    boolean firstLineOfRequestReceived = false;
    while(!firstLineOfRequestReceived) {
      requestString = fromClientStream.readLine();
      if(null == requestString) {
        debugm("The client has suddenly closed the connection!");
        return false;
      }
      else if(requestString.length() > 0) {
        firstLineOfRequestReceived = true;
      }
    }
    debugm("Received the first line of request string");
    String[] rArray = parseFirstLineInRequest(requestString);
    req = new Request(rArray[0], rArray[1], rArray[2]);
    readRequestHeaders();
    return true;
  }

  private void respondToRequest() throws IOException {
    Response resp = new Response(req, toClientStream);
    resp.respondToRequest();
  }

  private void closeCurrentConnectionIfPersistenceWasNotRequested() throws IOException {
    if(!req.keepAliveRequested()) {
      clientSocket.close();
    }
  }

  private boolean thereIsNoConnectionAlive() {
    return clientSocket == null || clientSocket.isClosed() || !clientSocket.isConnected();
  }

  /**
   * Waits for a client to connect, and then sets up stream objects for communication
    * in both directions.
   * @return {@code true} if the connection is successfully established.
   * @throws {@link IOException} if the server fails to accept the connection.
   */
  public boolean acceptConnectionFromClient() throws IOException {
    try {
      clientSocket = serverSocket.accept();
    } catch (IOException e) {
      debugm("Exception occurred while trying to accept connection. " + e);
      return false;
    }

    toClientStream = new DataOutputStream(clientSocket.getOutputStream());
    fromClientStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    return true;
  }

  private String[] parseFirstLineInRequest(String requestStr) {
    String[] rArray = requestStr.split(" ");
    return rArray;
  }

  /**
   * Reads request headers and parses them.
   */
  private void readRequestHeaders() throws IOException {
    boolean moreHeadersToBeRead = true;
    while (moreHeadersToBeRead) {
      String line = fromClientStream.readLine();
      if ((line == null) || (line.length() == 0)) {
        moreHeadersToBeRead = false;
      }
      else {
        parseHeaderLine(line);
      }
    }
  }

  /**
   * Parses the request string
   * @param r the request string
   */
  private void parseHeaderLine(String r) {
    String[] rArray = r.split(":", 2);
    req.addHeader(rArray[0], rArray[1].trim());
  }

  /**
   * Print debug message
   * @param message debug message
   */
  private void debugm(String message) {
    System.out.println(this.type + ": " + message);
  }

}
