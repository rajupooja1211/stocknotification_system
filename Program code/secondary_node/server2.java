import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public class server2 {

    private static List<String> subscribers = new ArrayList<>();
    private static List<String> topics = Arrays.asList("Economic Indicators", "Global Events");
    private static Map<String, List<String>> subscriptions = new ConcurrentHashMap<String, List<String>>();
    private static Map<String, List<String>> news = new HashMap<String, List<String>>() {
        {
            put("Economic Indicators",
                    Arrays.asList(
                            "GDP growth exceeds forecasts, expanding by 3.5% in the last quarter, driving stock market gains",
                            "Q2 GDP growth disappoints at 1.8%, leading to cautious trading as investors reevaluate earnings projections",
                            "Positive job report shows unemployment falling to 4.7%, lifting stock market sentiment and driving gains in financial sector"));
            put("Global Events", Arrays.asList(
                    "Fed announces interest rate hike of 0.25%, prompting a selloff in bond markets and causing tech stocks to tumble",
                    "Robust GDP growth of 4.2% fuels stock market rally, with the S&P 500 hitting record highs."));
        }
    };
    private static int currentLeader = 0;
    private static Map<String, Integer> flags = new ConcurrentHashMap<>();
    private static Map<String, List<String>> generatedEvents = new ConcurrentHashMap<>();

    public static void connectSubscriber(Socket connection, String data, String topic) {
        try {
            PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
            while (true) {
                flags.put(data, 0);
                subscribe(data, topic);
                String subscriptionInfo = "You are subscribing to this topic: " + subscriptions.get(data);
                out.println(subscriptionInfo);

                while (true) {
                    if (flags.get(data) == 1) {
                        notifySubscriber(connection, data);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void middlewareServerSender(Socket connection, String data) {
        try {
            PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
            while (true) {
                flags.put(data, 0);
                subscriptions.put(data, new ArrayList<>(topics));
                String subscriptionInfo = "You are subscribing to this topic: " + subscriptions.get(data);
                out.println(subscriptionInfo);

                while (true) {
                    if (flags.get(data) == 1) {
                        notifySubscriber(connection, data);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void middlewareServerReceiver(Socket connection, String data) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while (true) {
                String serverData = in.readLine();
                String[] m = serverData.split("-");
                if (m.length == 2) {
                    String topic = m[0];
                    String event = m[1];
                    publish(topic, event, 0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void threadedMasterSender(Socket ss) {
        try {
            PrintWriter out = new PrintWriter(ss.getOutputStream(), true);
            while (true) {
                flags.put("master", 0);
                subscriptions.put("master", new ArrayList<>(topics));
                String subscriptionInfo = "You are subscribing to this topic: " + subscriptions.get("master");
                out.println(subscriptionInfo);

                while (true) {
                    if (flags.get("master") == 1) {
                        notifySubscriber(ss, "master");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void threadedMasterReceiver(Socket ss) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(ss.getInputStream()));
            while (true) {
                String serverData = in.readLine();
                System.out.println("NOTIFICATION FROM MASTER: " + serverData);
                String[] p = serverData.split(" - ");
                if (p.length == 2) {
                    String topic = p[0];
                    String event = p[1];
                    publish(topic, event, 0);
                }
                p = serverData.split("-");
                if (p.length == 2 && p[0].equals("leader")) {
                    System.out.println("Received leader election message " + serverData);
                    synchronized (server2.class) {
                        if (Integer.parseInt(p[1]) > currentLeader) {
                            currentLeader = Integer.parseInt(p[1]);
                            System.out.println(serverData);
                        }
                        if (Integer.parseInt(p[1]) < currentLeader) {
                            String msg = "leader-" + currentLeader;
                            System.out.println(msg);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void subscribe(String name, String topic) {
        subscriptions.put(name, Arrays.asList(topic.split(",")));
    }

    public static void generateNews() {
        String topic = topics.get(new Random().nextInt(topics.size()));
        List<String> msgList = news.get(topic);
        String event = msgList.get(new Random().nextInt(msgList.size()));
        publish(topic, event, 1);
    }

    public static void publish(String topic, String event, int indicator) {
        event = topic + " - " + event;
        for (Map.Entry<String, List<String>> entry : subscriptions.entrySet()) {
            String name = entry.getKey();
            List<String> topicsSubscribed = entry.getValue();
            if (topicsSubscribed.contains(topic)) {
                generatedEvents.computeIfAbsent(name, k -> new ArrayList<>()).add(event);
                flags.put(name, 1);
            }
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(server2::generateNews, new Random().nextInt(6) + 30, TimeUnit.SECONDS);
    }

    public static void notifySubscriber(Socket connection, String name) {
        try {
            PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
            List<String> events = generatedEvents.get(name);
            if (events != null) {
                List<String> eventsCopy = new ArrayList<>(events); // Create a copy of the events list
                for (String msg : eventsCopy) {
                    out.println(msg);
                    events.remove(msg); // Remove the event from the original list
                }
                flags.put(name, 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java server2 <serverName>");
            return;
        }

        String serverName = args[0];
        int serverPort = 5041; // Secondary server port
        int masterPort = 5040; // Master server port
        String masterHost = "server1"; // Master server hostname or IP

        // Start generating news
        ScheduledExecutorService newsExecutor = Executors.newSingleThreadScheduledExecutor();
        newsExecutor.scheduleAtFixedRate(server2::generateNews, 0, 30 + new Random().nextInt(6), TimeUnit.SECONDS);

        try {
            // Connect to master server if this is a secondary server
            if (!"master".equalsIgnoreCase(serverName)) {
                Socket masterSocket = new Socket(masterHost, masterPort);
                PrintWriter masterOut = new PrintWriter(masterSocket.getOutputStream(), true);
                masterOut.println(serverName);
                new Thread(() -> threadedMasterReceiver(masterSocket)).start();
                new Thread(() -> threadedMasterSender(masterSocket)).start();
            }

            // Set up server socket for incoming subscriber connections
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server started on port: " + serverPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection: " + clientSocket.getInetAddress().getHostAddress() + ":"
                        + clientSocket.getPort());

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String data = in.readLine();

                if (data != null) {
                    System.out.println("Received data: " + data);
                    String[] parts = data.split("-");
                    if (parts.length >= 3 && "c".equals(parts[0])) {
                        // Handle subscriber connection
                        subscribers.add(parts[1]);
                        new Thread(() -> connectSubscriber(clientSocket, parts[1], parts[2])).start();
                    } else if (parts.length >= 2 && "s".equals(parts[0])) {
                        // Handle server connection
                        new Thread(() -> middlewareServerSender(clientSocket, parts[1])).start();
                        new Thread(() -> middlewareServerReceiver(clientSocket, parts[1])).start();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
