import java.util.Hashtable;

public class Request {
  private static boolean debug = false;
  private String rMethod;
  private String rPath;
  private String rVersion;
  public Hashtable<String, String> rHeader;

  /**
   * Constructor for Request Class
   * @param rMethod method type
   * @param rPath path of requested file
   * @param rVersion request version
   */
  public Request(String rMethod, String rPath, String rVersion) {
    this.rMethod = rMethod;
    this.rPath = rPath;
    this.rVersion = rVersion;
    this.rHeader = new Hashtable<>();
  }

  /**
   * Determine whether request is valid
   * @return boolean true if request is valid
   */
  public boolean isValid() {
    return (this.rMethod.equals("GET") || this.rMethod.equals("HEAD"))
          && (this.rVersion.equals("HTTP/1.0") || this.rVersion.equals("HTTP/1.1"));
  }

  /**
   * Add header content
   * @param header header type
   * @param value header value
   */
  public void addHeader(String header, String value) {
    rHeader.put(header, value);
  }

  /**
   * Returns true if the connection should be keep-alive.
   * The default is to return true (keep-alive)
   * unless the client specifies 'Connection: close' in such case it returns false.
   * @return the request connection type
   */
  public boolean keepAliveRequested() {
    return (this.rHeader.containsKey("Connection") &&
            this.rHeader.get("Connection").toLowerCase().matches("close")) ? false : true;
  }
  /**
   * Returns the request method.
   * @return the request method
   */
  public String getrMethod() {
    return rMethod;
  }

  /**
   * Returns the request path.
   * @return the request path.
   */
  public String getrPath() {
    return rPath;
  }

  /**
   * Returns the request version.
   * @return the request version.
   */
  public String getrVersion() {
    return rVersion;
  }

  /**
   * Returns the request headers hash table.
   * @return the request headers hash table
   */
  public Hashtable<String, String> getrHeader() {
    return rHeader;
  }
}
