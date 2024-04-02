import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private List<String> logs = new ArrayList<>();
    private final int HYDROGEN_PORT = 50000;
    private final int OXYGEN_PORT = 60000;
    private ExecutorService executor = Executors.newCachedThreadPool();

    private ConcurrentLinkedQueue<String> hydrogenQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> oxygenQueue = new ConcurrentLinkedQueue<>();

    private AtomicInteger numHydrogen = new AtomicInteger(0);
    private AtomicInteger numOxygen = new AtomicInteger(0);
    private AtomicInteger numH2OMolecules = new AtomicInteger(0);

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void start() {
        try {
            ServerSocket HydrogenServerSocket = new ServerSocket(HYDROGEN_PORT);
            ServerSocket OxygenServerSocket = new ServerSocket(OXYGEN_PORT);
            System.out.println("Server is listening...");

            handleConnections(HydrogenServerSocket, true);
            handleConnections(OxygenServerSocket, false);

        } catch (IOException e) {
            System.err.println("Server start error: " + e.getMessage());
        }
    }

    private void handleConnections(ServerSocket serverSocket, boolean isHydrogen) {
        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    if (isHydrogen) {
                        System.out.println("Connection from Hydrogen Client: " + socket);
                        executor.execute(new ClientThread(socket, true));
                    } else {
                        System.out.println("Connection from Oxygen Client: " + socket);
                        executor.execute(new ClientThread(socket, false));
                    }
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        });
    }

    private void logAction(String id, String action) {
        String timestamp = sdf.format(new Date());
        String logEntry = String.format("(%s, %s, %s)", id, action, timestamp);
        System.out.println(logEntry); // Print log for visibility
        logs.add(logEntry);
    }

    private class ClientThread implements Runnable {
        private Socket socket;
        private boolean isHydrogen;

        public ClientThread(Socket socket, boolean isHydrogen) {
            this.socket = socket;
            this.isHydrogen = isHydrogen;
        }

        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                while (true) {
                    String received = (String) in.readObject();
                    if (received != null && !received.isEmpty()) {
                        String id = received.split(", ")[0];
                        logAction(id, "request");
                        if (isHydrogen) {
                            hydrogenQueue.add(received);
                            numHydrogen.incrementAndGet();
                        } else {
                            oxygenQueue.add(received);
                            numOxygen.incrementAndGet();
                        }
                        tryFormingBonds();
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void tryFormingBonds() {
        while (numHydrogen.get() >= 2 && numOxygen.get() >= 1) {
            chemicalBond();
        }
    }

    private void chemicalBond() {
        if (numHydrogen.get() >= 2 && numOxygen.get() >= 1) {
            String hMole1 = hydrogenQueue.poll().split(", ")[0];
            String hMole2 = hydrogenQueue.poll().split(", ")[0];
            String oMole = oxygenQueue.poll().split(", ")[0];

            logAction(hMole1, "bonded");
            logAction(hMole2, "bonded");
            logAction(oMole, "bonded");

            numHydrogen.addAndGet(-2);
            numOxygen.decrementAndGet();

            numH2OMolecules.incrementAndGet();
        }
    }
}
