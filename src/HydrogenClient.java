import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HydrogenClient {
    private static final int HYDROGEN_PORT = 50000;
    private static final String SERVER_IP = "localhost";
    private ExecutorService executor;
    private Socket socket;

    public HydrogenClient() {
        // Initialize with a fixed number of threads based on available resources
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void start() {
        int ID = 1;

        try {
            socket = new Socket(SERVER_IP, HYDROGEN_PORT);
            System.out.println("Connected to server");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            // Use executor to start the listening thread
            executor.submit(new ListenThread(socket));

            Scanner scan = new Scanner(System.in);
            System.out.print("Enter the number of hydrogen molecules: ");
            int n = scan.nextInt();
            while (n <= 0) {
                System.out.println("Enter a valid number of hydrogen molecules");
                n = scan.nextInt();
            }

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < n; i++) {
                String request = "H-" + (ID++) + ", request";
                out.writeObject(request);
                out.flush(); // Ensure data is sent immediately
            }

            long endTime = System.currentTimeMillis();
            System.out.println("HYDROGEN THREAD END");
            System.out.println("Runtime: " + (endTime - startTime) + " milliseconds");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ListenThread implements Runnable {
        private final Socket socket;

        public ListenThread(Socket socket) {
            this.socket = socket;
        }

        @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String received = (String) in.readObject();
                    System.out.println(received);
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                    break; // Exit the loop if an exception occurs
                }
            }
        } catch (IOException e) {
            System.err.println("IOException in ListenThread: " + e.getMessage());
        }
    }
    }

    public void shutdown() {
        try {
            System.out.println("Shutting down client...");
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            socket.close();
        } catch (InterruptedException | IOException e) {
            System.err.println("Interrupted during shutdown.");
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) {
        HydrogenClient client = new HydrogenClient();
        client.start();
        client.shutdown();
    }
}
