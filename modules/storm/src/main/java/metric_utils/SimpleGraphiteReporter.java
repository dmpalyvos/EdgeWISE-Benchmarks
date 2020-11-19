package metric_utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class SimpleGraphiteReporter {

  private final int graphitePort;
  private final String graphiteHost;
  private Socket socket;
  private DataOutputStream output;

  public static void main(String[] args) throws IOException {
    SimpleGraphiteReporter reporter = new SimpleGraphiteReporter("129.16.20.158", 2003);
    reporter
        .report(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), "lachesis.test", 100);
  }

  public SimpleGraphiteReporter(String graphiteHost, int graphitePort) {
    this.graphiteHost = graphiteHost;
    this.graphitePort = graphitePort;
  }

  public void open() {
    try {
      socket = new Socket(graphiteHost, graphitePort);
      output = new DataOutputStream(socket.getOutputStream());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public void report(long timestampSeconds, String key, Object value) throws IOException {
    output.writeBytes(String.format("%s %s %d\n", key, value, timestampSeconds));
  }

  public void close() {
    try {
      output.flush();
      output.close();
      socket.close();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
