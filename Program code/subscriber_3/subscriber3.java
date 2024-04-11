import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class subscriber3 {
    public static void main(String[] args) {
        // Check for the correct usage and presence of the subscriber name argument
        if (args.length < 1) {
            System.err.println("Usage: java SubscriberClient <SubscriberName>");
            System.exit(1);
        }

        // Extract subscriber name from command-line arguments
        String subscriberName = args[0];
        System.out.println("Subscriber is: " + subscriberName);

        // Server details
        String host = "server2"; // Replace with actual host name or IP address
        int port = 5041; // Replace with the actual port number

        try {
            // Establish connection to the server
            Socket socket = new Socket(host, port);
            System.out.println("Connected to " + host + " on port " + port);

            // Setup output stream to send data to the server
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            // Setup input stream to receive data from the server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send the subscriber name to the server
            out.println(subscriberName);

            // Continuously read and print messages from the server
            String data;
            while ((data = in.readLine()) != null) {
                System.out.println(data);
            }

            // Close the socket and streams
            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            System.err.println("Host unknown: " + host);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            System.exit(1);
        }
    }
}
