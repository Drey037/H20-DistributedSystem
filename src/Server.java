import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


public class Server {
    private List<String> logs;
    private final int HYDROGEN_PORT = 50000;
    private final int OXYGEN_PORT = 60000;
    private Socket HydrogenSocket;
    private Socket OxygenSocket;

    // Use ConcurrentLinkedQueue for queues
    private ConcurrentLinkedQueue<String> hydrogenQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> oxygenQueue = new ConcurrentLinkedQueue<>();

    // Use AtomicInteger for counters
    private AtomicInteger numHydrogen = new AtomicInteger(0);
    private AtomicInteger numOxygen = new AtomicInteger(0);
    private AtomicInteger numH2OMolecules = new AtomicInteger(0);

    public void start() {
        logs = new ArrayList<>();

        try {
            ServerSocket HydrogenServerSocket = new ServerSocket(HYDROGEN_PORT);
            ServerSocket OxygenServerSocket = new ServerSocket(OXYGEN_PORT);
            InetAddress inetAddress = InetAddress.getLocalHost();
            System.out.println("Server opened at: " + inetAddress.getHostAddress());
            System.out.println("Server is listening...");

            // Connection with Hydrogen
            HydrogenSocket = HydrogenServerSocket.accept();
            System.out.println("Connection from Hydrogen Client: " + HydrogenSocket);

            // Connection with Oxygen
            OxygenSocket = OxygenServerSocket.accept();
            System.out.println("Connection from Oxygen Client: " + OxygenSocket);

            ObjectOutputStream outO = new ObjectOutputStream(OxygenSocket.getOutputStream());
            ObjectOutputStream outH = new ObjectOutputStream(HydrogenSocket.getOutputStream());

            // Start threads
            new OxygenThread(OxygenSocket).start();
            new HydrogenThread(HydrogenSocket).start();

            System.out.println("Start Bonding");

            while (true) {
                // Check if there are bonds to be made
                if (numHydrogen.get() >= 2 && numOxygen.get() >= 1) {
                    chemicalBond(outO, outH);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("MASTER: Done");
    }

    public void chemicalBond(ObjectOutputStream outO, ObjectOutputStream outH) {
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());

        // Push out one molecule of Oxygen
        String oMole = oxygenQueue.poll();
        numOxygen.decrementAndGet();

        logs.add(oMole + ", bonded, " + timeStamp);
        try {
            outO.writeObject(oMole + ", bonded, " + timeStamp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Push out two molecules of Hydrogen
        String hMole1 = hydrogenQueue.poll();
        String hMole2 = hydrogenQueue.poll();
        numHydrogen.addAndGet(-2);

        logs.add(hMole1 + ", bonded, " + timeStamp);
        logs.add(hMole2 + ", bonded, " + timeStamp);

        try {
            outH.writeObject(hMole1 + ", bonded, " + timeStamp);
            outH.writeObject(hMole2 + ", bonded, " + timeStamp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        numH2OMolecules.incrementAndGet();
        System.out.println("# H2O Bonded: " + numH2OMolecules.get());
    }

    public class HydrogenThread extends Thread {
        protected Socket socket;

        public HydrogenThread(Socket hydrogen) {
            this.socket = hydrogen;
        }

        public void run() {
            try {
                ObjectInputStream inHydrogen = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    try {
                        String received = (String) inHydrogen.readObject();

                        if (received != null && !received.isEmpty()) {
                            logs.add(received);
                        }
                        String name = received.split(", ")[0];
                        hydrogenQueue.add(name);
                        numHydrogen.incrementAndGet();

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class OxygenThread extends Thread {
        protected Socket socket;

        public OxygenThread(Socket oxygen) {
            this.socket = oxygen;
        }

        public void run() {
            try {
                ObjectInputStream inOxygen = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    try {
                        String received = (String) inOxygen.readObject();

                        if (received != null && !received.isEmpty()) {
                            logs.add(received);
                        }
                        String name = received.split(", ")[0];
                        oxygenQueue.add(name);
                        numOxygen.incrementAndGet();

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
