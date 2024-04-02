import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int HYDROGEN_PORT = 50000;
    private static final int OXYGEN_PORT = 60000;
    private ExecutorService executor;
    private final AtomicInteger numHydrogen = new AtomicInteger(0);
    private final AtomicInteger numOxygen = new AtomicInteger(0);
    private final AtomicInteger numH2OMolecules = new AtomicInteger(0);

    private final ConcurrentLinkedQueue<String> hydrogenQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> oxygenQueue = new ConcurrentLinkedQueue<>();

    public Server() {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    public void start() {
        try {
            ServerSocket hydrogenServerSocket = new ServerSocket(HYDROGEN_PORT);
            ServerSocket oxygenServerSocket = new ServerSocket(OXYGEN_PORT);
            System.out.println("Server is listening...");

            executor.execute(() -> handleConnections(hydrogenServerSocket, true));
            executor.execute(() -> handleConnections(oxygenServerSocket, false));
        } catch (IOException e) {
            System.err.println("Server start error: " + e.getMessage());
        }
    }

    private void handleConnections(ServerSocket serverSocket, boolean isHydrogen) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientThread(clientSocket, isHydrogen));
            }
        } catch (IOException e) {
            System.err.println("Error accepting client connection: " + e.getMessage());
        }
    }

    private class ClientThread implements Runnable {
        private final Socket socket;
        private final boolean isHydrogen;

        public ClientThread(Socket socket, boolean isHydrogen) {
            this.socket = socket;
            this.isHydrogen = isHydrogen;
        }

        @Override
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
                System.err.println("Error in ClientThread: " + e.getMessage());
            }
        }
    }

    private void logAction(String id, String action) {
        System.out.printf("(%s, %s, %s)\n", id, action, new Date());
    }

    private void tryFormingBonds() {
        if (numHydrogen.get() >= 2 && numOxygen.get() >= 1) {
            chemicalBond();
        }
    }

    private synchronized void chemicalBond() {
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
    
            System.out.println("# H2O Bonded: " + numH2OMolecules.get());
        }
    }
}