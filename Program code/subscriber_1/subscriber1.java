import java.io.*;
import java.net.*;

public class subscriber1 {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java SubscriberClient <SubscriberName>");
            System.exit(1);
        }

        String subscriberName = args[0];
        System.out.println("Subscriber is: " + subscriberName);

        String host = "server2"; // Change to the actual server hostname or IP address
        int port = 5041; // Ensure this matches the server's listening port

        try (Socket socket = new Socket(host, port)) {
            OutputStream outToServer = socket.getOutputStream();
            PrintWriter out = new PrintWriter(outToServer, true);
            InputStream inFromServer = socket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inFromServer));

            // Send subscriber name to the server
            out.println(subscriberName);

            // Continuously read and print messages from the server
            String data;
            while ((data = in.readLine()) != null) {
                System.out.println(data);
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + host);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    host);
            System.exit(1);
        }
    }
}
