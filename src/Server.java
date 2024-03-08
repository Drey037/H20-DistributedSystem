import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    private List<String> logs;

    private static final Object logLock = new Object();
    private static final Object bondLock = new Object();

    private static final Object oLock = new Object();
    private static final Object hLock = new Object();
    private final int HYDROGEN_PORT = 50000;

    private final int OXYGEN_PORT = 60000;

    private Socket HydrogenSocket;
    private Socket OxygenSocket;

    private Queue<String> hydrogenQueue;
    private Queue<String> oxygenQueue;

    private volatile int numHydrogen;
    private volatile int numOxygen;


    public void start() {
        logs = new ArrayList<>();
        hydrogenQueue = new LinkedList<>();
        oxygenQueue = new LinkedList<>();

        try {
            ServerSocket HydrogenServerSocket = new ServerSocket(HYDROGEN_PORT);
            ServerSocket OxygenServerSocket = new ServerSocket(OXYGEN_PORT);
            InetAddress inetAddress = InetAddress.getLocalHost();
            System.out.println("Server opened at: " + inetAddress.getHostAddress());
            System.out.println("Server is listening...");

            // Connection with Hydrogen
            HydrogenSocket = HydrogenServerSocket.accept();
            System.out.println("Connection from: " + HydrogenSocket);

            //Connection with Oxygen
            OxygenSocket = OxygenServerSocket.accept();
            System.out.println("Connection from: " + OxygenSocket);

            ObjectOutputStream outO = new ObjectOutputStream(OxygenSocket.getOutputStream());
            ObjectOutputStream outH = new ObjectOutputStream(HydrogenSocket.getOutputStream());

            // Start threads
            new oxygenThread(OxygenSocket).start();
            new hydrogenThread(HydrogenSocket).start();

            while (true) {
                // Check if there are bonds to be made
                if (numHydrogen >= 2 && numOxygen >= 1) {
                    chemicalBond(outO, outH);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("MASTER: Done");

    }

    public void chemicalBond(ObjectOutputStream outO , ObjectOutputStream outH) {
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
        synchronized (oLock) {
            // Push out one molecule of Oxygen
            String oMole = oxygenQueue.poll();
            numOxygen--;

            logs.add(oMole + ", bonded, " + timeStamp);
            System.out.println(oMole + ", bonded, " + timeStamp);
            try {
                outO.writeObject(oMole + ", bonded, " + timeStamp);
            }
            catch(IOException e) {
                return;
            }
        }

        synchronized (hLock) {
            // Push out two molecules of Hydrogen
            String hMole1 = hydrogenQueue.poll();
            String hMole2 = hydrogenQueue.poll();
            numHydrogen--;
            numHydrogen--;
            logs.add(hMole1 + ", bonded, " + timeStamp);
            logs.add(hMole2 + ", bonded, " + timeStamp);

            System.out.println(hMole1 + ", bonded, " + timeStamp);
            System.out.println(hMole2 + ", bonded, " + timeStamp);
            try {
                outH.writeObject(hMole1 + ", bonded, " + timeStamp);
                outH.writeObject(hMole2 + ", bonded, " + timeStamp);
            }
            catch(IOException e) {
                return;
            }
        }
    }

    public class hydrogenThread extends Thread {
        protected Socket socket;

        public hydrogenThread(Socket hydrogen) {
            this.socket = hydrogen;
        }

        public void run() {
            try {
                // Slave server connection
                ObjectInputStream inHydrogen = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    try {
                        String received =  (String) inHydrogen.readObject();

                        if (received != null && !received.isEmpty()) {
                            synchronized (logLock) {
                                logs.add(received);
                                System.out.println(received);
                            }
                        }
                        synchronized (hLock) {
                            // Hydrogen-n Request -> Hydrogen-n
                            String name = received.split(", ")[0];
                            hydrogenQueue.add(name);
                            numHydrogen++;
                        }

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            } catch (IOException e) {
                return;
            }
        }
    }

    public class oxygenThread extends Thread {
        protected Socket socket;

        public oxygenThread(Socket oxygen) {
            this.socket = oxygen;
        }

        public void run() {
            try {
                // Slave server connection
                ObjectInputStream inOxygen = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    try {
                        String received =  (String) inOxygen.readObject();

                        if (received != null && !received.isEmpty()) {
                            //TODO: Optional - Add a timestamp to the string to be added to the logs
                            synchronized (logLock) {
                                logs.add(received);
                                System.out.println(received);
                            }
                        }
                        synchronized (oLock) {
                            String name = received.split(", ")[0];
                            oxygenQueue.add(name);
                            numOxygen++;
                        }

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            } catch (IOException e) {
                return;
            }
        }
    }
}
