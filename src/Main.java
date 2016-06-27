import java.util.Map;

public class Main {
  private static Server server;
  private static SSLServer sslServer;
  private static Map<String, String> commandOptions;
  private static String serverPortOption = "--serverPort";
  private static String sslServerPortOption = "--sslServerPort";
  private static int serverPort = -1;
  private static int sslServerPort = -1;

  /**
   * Create server from command line options
   * @param argv command line arguments
   */
  public static void main(String argv[]) {
    createServersFromCommandLineOptions(argv);
    new Thread(Main.server).start();
    new Thread(Main.sslServer).start();
  }

  /**
   * Creates a Server from the options received in the command line.
   * @param argv the options received in the command line.
   */
  private static void createServersFromCommandLineOptions(String[] argv) {
    initializeServerPorts(argv);
    Main.server = new Server(serverPort); //normal http sever
    Main.sslServer = new SSLServer(sslServerPort); //secure https server
  }

  /**
   * Initialize server ports
   * @param argv command line arguments
   */
  private static void initializeServerPorts(String[] argv) {
    commandOptions = Utils.parseCmdlineFlags(argv);
    checkAllPortOptionsAreGiven();
    try {
      serverPort = Integer.parseInt(commandOptions.get(serverPortOption));
      sslServerPort = Integer.parseInt(commandOptions.get(sslServerPortOption));
    } catch (NumberFormatException e) {
      System.out.println("Invalid port number! Must be an integer.");
      System.exit(-1);
    }
  }

  /**
   * Check port options
   */
  private static void checkAllPortOptionsAreGiven() {
    if (!commandOptions.containsKey(serverPortOption) || !commandOptions.containsKey(sslServerPortOption)) {
      System.out.println("usage: Main --serverPort=2345 --sslServerPort=3456");
      System.exit(-1);
    }
  }
}
