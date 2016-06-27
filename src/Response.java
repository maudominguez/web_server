import java.util.*;
import java.io.*;

public class Response {

  private static boolean debug = true;
  private static Hashtable<Integer, String> htStatus;
  private static final String sRedirectFile = "www/redirect.defs";
  private static Hashtable<String,String> htRedirect;
  private static StringBuffer sb;

  private Request r;
  private Hashtable<String, String> responseHeader;
  private String path;
  public DataOutputStream dataOutputStream;

  /**
   * Constructor for Response class
   * @param r Client's request
   * @param outputStream DataOutputStream
   */
  public Response(Request r, DataOutputStream outputStream) {
    this.r = r;
    this.path = r.getrPath();
    this.responseHeader = new Hashtable<>();
    this.dataOutputStream = outputStream;
    initializeHeader();
    initializeStatusTable();
    initializeRedirect();
  }

  /**
   * Initializes the response headers hash table
   */
  private void initializeHeader() {
    responseHeader.put("Date", new Date().toString());
    responseHeader.put("Content-Length", "0");
    responseHeader.put("Connection", r.keepAliveRequested() ? "keep-alive" : "close");
    responseHeader.put("Server", "Java Server");
  }

  /**
   * Initializes the status hash table
   */
  private void initializeStatusTable() {
    this.htStatus = new Hashtable<>();
    this.htStatus.put(200, "OK");
    this.htStatus.put(301, "Moved Permanently");
    this.htStatus.put(400, "Bad Request");
    this.htStatus.put(403, "Forbidden");
    this.htStatus.put(404, "Not Found");
    this.htStatus.put(406, "Not Acceptable");
    this.htStatus.put(500, "Internal Server Error");
    this.htStatus.put(505, "HTTP Version Not Supported");
  }

  /**
   * Initializes the redirect hash table.
   */
  private void initializeRedirect() {
    htRedirect = new Hashtable<>();
    BufferedReader brRedirectFile;
    try {
      brRedirectFile = new BufferedReader(new InputStreamReader(new FileInputStream(sRedirectFile)));
      String line;
      while ((line = brRedirectFile.readLine()) != null) {
        String[] redirectLineArray = line.split(" ");
        htRedirect.put(redirectLineArray[0], redirectLineArray[1]);
      }
    } catch (Exception e) {
      System.out.println("Error: could not read www/redirect.defs file. Please make sure it exists and has the correct format.");
    }
  }

  /**
   * Is the path contained in the redirect hash table?
   * @param path File path
   * @return true if the path is contained in the redirect table, false otherwise
   */
  private static boolean isRedirect(String path) {
    return htRedirect.containsKey(path);
  }

  /**
   * Get the redirect address for path
   * @param path File path
   * @return the redirect address for path
   */
  private static String getRedirect(String path) {
    return htRedirect.get(path);
  }

  /**
   * Adds to the headers table the appropriate Mime type for the file.
   * @param f file
   */
  private void addContentType(File f) {
    responseHeader.put("Content-Type", getMimeType(f.getAbsolutePath()));
  }

  /**
   * Fill and send the response to the client.
   */
  private void respondToValidRequest() {
    StringBuffer sb = new StringBuffer();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2000);

    path = path.startsWith("/")? path : ("/"+path);

    try {
      if (isRedirect(path)) {
        respondWithErrorCode(301);
        return;
      }

      //attach www to path
      path = "www" + path;
      File requestFile = new File(path);

      //prevent redirect.defs from being read
      if (path.equals("www/redirect.defs") || (!requestFile.exists())) {
        respondWithErrorCode(404);
        return;
      }

      //examine whether file is directory
      if (requestFile.isDirectory())
        path = (path.endsWith("/")) ? (path + "index.html") : (path + "/index.html");
      try {
        requestFile = new File(path);
        if (requestFile.exists()) {
          addContentType(requestFile);          //add file content type

          //add file content length
          responseHeader.put("Content-Length", String.valueOf(requestFile.length()));

          sb.append(r.getrVersion() + " 200 OK \r\n");

          for (String h : responseHeader.keySet())   //Display all headers
            sb.append(h + ": " + responseHeader.get(h) + " \r\n");

          sb.append("\r\n");  //Blank line between header and file content of GET request

          //process if method is GET
          if (r.getrMethod().equals("GET")) {
            String line;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(requestFile));
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(requestFile));
            byte[] bArray = new byte[2000];

            if (!getMimeType(requestFile.getAbsolutePath()).contains("text")) { //if not text
              while (true) {
                int read = bufferedInputStream.read(bArray);
                if (read == -1) { break; } //if end of file
                else { byteArrayOutputStream.write(bArray, 0, read); }
              }
            } else { //if text/html
              int character = bufferedReader.read();
              while(character != -1) {
                sb.append((char)character);
                character = bufferedReader.read();
              }
            }
          }
        } else {
          respondWithErrorCode(404);
        }
      } catch (Exception e) {
        respondWithErrorCode(500); return;
      }
    } catch (Exception e) {
      respondWithErrorCode(500); return;
    }

    try {
      dataOutputStream.writeBytes(sb.toString());

      byteArrayOutputStream.writeTo(dataOutputStream);
      byteArrayOutputStream.flush();

      if (debug) { System.out.println(sb.toString()); }
    } catch (Exception e) {
      respondWithErrorCode(500);
    }
  }

  /**
   * Respond depending on whether request is valid
   */
  public void respondToRequest() {
    if (r.isValid())
      respondToValidRequest();
    else
      respondWithErrorCode(403);
  }

  /**
   * Respond with given error code
   * @param statusCode error code
   */
  private void respondWithErrorCode(int statusCode) {
    sb = new StringBuffer("");
    String html = getHtmlResponseToInvalidRequest(statusCode);
    addHeadersToResponseWithErrorCode(statusCode, html);
    addHTMLToResponseWithErrorCode(html);
    try {
      dataOutputStream.writeBytes(sb.toString());
    } catch (Exception e) {
      System.out.println("Error I/O in respondToInvalidRequest");
    }
  }

  /**
   * Add headers to error response page
   * @param statusCode error code
   * @param html
   */
  private void addHeadersToResponseWithErrorCode(int statusCode, String html) {
    sb.append(r.getrVersion() + " " + statusCode + " " + htStatus.get(statusCode) + "\r\n");
    sb.append("Content-type: text/html"+ "\r\n");
    sb.append("Connection: " + (r.keepAliveRequested() ? "keep-alive" : "close") + "\r\n");
    if (!r.getrMethod().equals("HEAD"))
      sb.append("Content-Length: " + html.getBytes().length + "\r\n");
    if (301 == statusCode)
      sb.append("Location: " +  getRedirect(path) + "\r\n");
    sb.append("\r\n");
  }

  /**
   * Add HTML to error response page
   * @param html HTML portion of response page
   */
  private void addHTMLToResponseWithErrorCode(String html) {
    if (!r.getrMethod().equals("HEAD"))
      sb.append(html);
  }

  /**
   * Get HTML portion of response to invalid request
   * @param statusCode error code
   * @return string containing HTML portion of response
   */
  private String getHtmlResponseToInvalidRequest(int statusCode) {
    StringBuffer html = new StringBuffer("");
    html.append("<!DOCTYPE html>\r\n");
    html.append("<head><title>Error " + statusCode + " " + htStatus.get(statusCode) + "</title></head>" + "\r\n");
    html.append("<body><h3>Error "+statusCode+"</h3>\r\n");
    html.append("<br>" + htStatus.get(statusCode));
    html.append("</body></html>\r\n");
    return html.toString();
  }

  /**
   * Returns a string with the appropriate mime type for the received format
   * @param s file as string
   * @return the mime type for the format
   */
  private static String getMimeType(String s)  {
    if   (s.endsWith(".html") || s.endsWith(".htm"))  return "text/html";
    else if (s.endsWith(".pdf"))                      return "application/pdf";
    else if (s.endsWith(".png"))                      return "image/png";
    else if (s.endsWith(".jpg")||s.endsWith(".jpeg")) return "image/jpeg";
    else if (s.endsWith(".txt"))                      return "text/plain";
    else                                              return "text/plain";
  }
}
