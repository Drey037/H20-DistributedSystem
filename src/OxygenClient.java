import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OxygenClient {
    private static final int OXYGEN_PORT = 60000;
    private static final String SERVER_IP = "localhost";
    private ExecutorService executor;
    private Socket socket;

    public OxygenClient() {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void start() {
        int ID = 1;

        try {
            socket = new Socket(SERVER_IP, OXYGEN_PORT);
            System.out.println("Connected to server");
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            executor.submit(new ListenThread(socket));

            Scanner scan = new Scanner(System.in);
            System.out.print("Enter the number of oxygen molecules: ");
            int m = scan.nextInt();

            while (m <= 0) {
                System.out.println("Enter a valid number of oxygen molecules");
                m = scan.nextInt();
            }

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < m; i++) {
                String request = "O-" + (ID++) + ", request";
                out.writeObject(request);
                out.flush();
            }

            long endTime = System.currentTimeMillis();
            System.out.println("OXYGEN THREAD END");
            System.out.println("Runtime: " + (endTime - startTime) + " milliseconds");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ListenThread implements Runnable {
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
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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
        OxygenClient client = new OxygenClient();
        client.start();
        client.shutdown();
    }
}
