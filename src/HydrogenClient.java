import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HydrogenClient {
    private final int HYDROGEN_PORT = 50000;

    private final String SERVER_IP = "localhost";

    private ExecutorService executor;

    public HydrogenClient() {
        // Initialize the ExecutorService
        executor = Executors.newCachedThreadPool();
    }

    public void start() {
        int ID = 1; // Start ID index system for molecules

        try {
            String address = "localhost";
            Socket socket = new Socket(address, HYDROGEN_PORT);
            System.out.println("Connected to server");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            Random r = new Random(12345);

            // Use executor to start the listening thread
            executor.submit(new ListenThread(socket));

            // User Input
            Scanner scan = new Scanner(System.in);
            int n = 0;
            do {
                System.out.print("Enter the number of hydrogen molecules: ");
                n = scan.nextInt();

                if (n <= 0) {
                    System.out.println("Enter a valid number of hydrogen molecules");
                }
            } while (n <= 0);

            // Record the start time
            long startTime = System.currentTimeMillis();

            while (ID <= n) {
                int random_time = r.nextInt(1000 - 50) + 50;
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());

                String request = "H-" + ID + ", request, " + timeStamp;
                out.writeObject(request);
                ID++;

                // Random sleep
                Thread.sleep(random_time);
            }

            long endTime = System.currentTimeMillis();
            System.out.println("HYDROGEN THREAD END");
            System.out.println("Runtime: " + (endTime - startTime) + " milliseconds");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class ListenThread implements Runnable {
        protected Socket socket;

        public ListenThread(Socket hydrogen) {
            this.socket = hydrogen;
        }

        @Override
        public void run() {
            try (ObjectInputStream inHydrogen = new ObjectInputStream(socket.getInputStream())) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String received = inHydrogen.readObject().toString();

                        // Process the received data
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
        } catch (InterruptedException e) {
            System.err.println("Interrupted during shutdown.");
            executor.shutdownNow();
        }
    }    
}

