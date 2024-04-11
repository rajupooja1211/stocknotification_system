import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class server1 {

    private static List<String> subscribers = new ArrayList<>();
    private static List<String> topics = Arrays.asList("Economic Reports", "Sector Performance", "Inflation");
    private static Map<String, List<String>> subscriptions = new ConcurrentHashMap<String, List<String>>();
    private static Map<String, List<String>> news = new HashMap<String, List<String>>() {
        {
            put("Economic Reports",
                    Arrays.asList("Apple reports robust earnings, driven by strong iPhone and services sales.",
                            "Samsung's profit soars as semiconductor demand fuels growth.",
                            "OpenAI showcases impressive revenue growth, fueled by AI technology advancements."));
            put("Sector Performance",
                    Arrays.asList(
                            "Technology sector surges 10% on strong earnings reports from major FAANG stocks, propelling the Nasdaq to record highs amid bullish sentiment.",
                            "Healthcare sector lags behind as regulatory uncertainty weighs on pharmaceutical and biotech stocks, dragging down the S&P 500 index despite positive economic data.",
                            "Financial sector rallies 8% on hopes of deregulation and interest rate hikes, outperforming other sectors as banks and insurers lead the charge in a rising rate environment."));
            put("Inflation", Arrays.asList(
                    "Inflation spikes to 5.2% year-over-year, exceeding expectations and triggering concerns of stagflation, leading to volatility in energy and consumer goods stocks.",
                    "Moderate inflation of 2.1% eases market fears, prompting a rally in technology and healthcare sectors as investors gain confidence in sustained economic growth.",
                    "Rising inflation hits 3.8%, driving investors to defensive stocks such as utilities and consumer staples, while cyclical sectors like industrials and materials face headwinds."));
        }
    };
    private static int currentLeader = 1;
    private static boolean firstTime = false;
    private static Map<String, Integer> flags = new ConcurrentHashMap<>();
    private static Map<String, List<String>> generatedEvents = new ConcurrentHashMap<>();
    private static Random random = new Random(); // Define random here

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
                if (!firstTime) {
                    String msg = "leader-1";
                    System.out.println("Initiating leader election message " + msg);
                    out.println(msg);
                    firstTime = true;
                }
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
                    if (m[0].equals("leader")) {
                        System.out.println("Received leader election message " + serverData);
                        if (Integer.parseInt(m[1]) > currentLeader) {
                            currentLeader = Integer.parseInt(m[1]);
                            PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
                            out.println(serverData);
                        }
                        if (Integer.parseInt(m[1]) == currentLeader) {
                            System.out.println("server1 elected as a leader");
                        }
                    } else {
                        String topic = m[0];
                        String event = m[1];
                        publish(topic, event, 0);
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
        Random random = new Random();
        String topic = topics.get(random.nextInt(topics.size()));
        List<String> msgList = news.get(topic);
        String event = msgList.get(random.nextInt(msgList.size()));
        publish(topic, event, 1);
    }

    public static void publish(String topic, String event, int indicator) {
        event = topic + " - " + event;
        if (indicator == 1) {
            for (Map.Entry<String, List<String>> entry : subscriptions.entrySet()) {
                String name = entry.getKey();
                List<String> topicsSubscribed = entry.getValue();
                if (topicsSubscribed.contains(topic)) {
                    generatedEvents.computeIfAbsent(name, k -> new ArrayList<>()).add(event);
                    flags.put(name, 1);
                }
            }
        } else {
            for (Map.Entry<String, List<String>> entry : subscriptions.entrySet()) {
                String name = entry.getKey();
                List<String> topicsSubscribed = entry.getValue();
                if (subscribers.contains(name) && topicsSubscribed.contains(topic)) {
                    generatedEvents.computeIfAbsent(name, k -> new ArrayList<>()).add(event);
                    flags.put(name, 1);
                }
            }
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(server1::generateNews, random.nextInt(6) + 20, TimeUnit.SECONDS);
    }

    public static void notifySubscriber(Socket connection, String name) {
        try {
            PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
            List<String> events = generatedEvents.get(name);
            if (events != null) {
                Iterator<String> iterator = events.iterator();
                while (iterator.hasNext()) {
                    String msg = iterator.next();
                    out.println(msg);
                    iterator.remove(); // Safely remove the element from the list
                }
                flags.put(name, 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = 5040;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Socket is bind to the port: " + port);
            System.out.println("Socket is now listening for new connection...");
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(server1::generateNews, new Random().nextInt(6) + 20, TimeUnit.SECONDS);

            while (true) {
                Socket connection = serverSocket.accept();
                System.out.println(
                        "Connected to: " + connection.getInetAddress().getHostAddress() + ":" + connection.getPort());
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String data = in.readLine();

                if (data != null) {
                    System.out.println("Welcome " + data);
                }
                String[] l = data.split("-");
                if (l[0].equals("c")) {
                    subscribers.add(l[1]);
                    new Thread(() -> connectSubscriber(connection, l[1], l[2])).start();
                }
                if (l[0].equals("s")) {
                    new Thread(() -> middlewareServerSender(connection, l[1])).start();
                    new Thread(() -> middlewareServerReceiver(connection, l[1])).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
