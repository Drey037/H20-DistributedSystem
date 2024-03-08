import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Scanner;

public class HydrogenClient {
    private final int HYDROGEN_PORT = 50000;

    private final String SERVER_IP = "localhost";
    public void start() {
        int ID = 1; // START ID INDEX SYSTEM FOR MOLECULES

        try {
            String address = "localhost";
            //InetAddress address = InetAddress.getByName(SERVER_IP); // Enter host ip address
            Socket socket = new Socket(address, HYDROGEN_PORT);
            System.out.println("Connected to server");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            Random r = new Random(12345);

            // new thread for a client
            new listenThread(socket).start();

            // User Input
            Scanner scan = new Scanner(System.in);
            int n = 0;
            do {
                n = scan.nextInt();

                if (n <= 0)
                    System.out.println("Enter a valid number of hydrogen molecules");
            }
            while (n <= 0);

            // Record the start time
            long startTime = System.currentTimeMillis();

            // Limit to 5000
            while (ID <= n) {
                int random_time = r.nextInt(1000-50) + 50;
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

            socket.close();
        } catch (IOException e) {
            //e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public class listenThread extends Thread {
        protected Socket socket;

        public listenThread(Socket hydrogen) {
            this.socket = hydrogen;
        }

        public void run() {
            try {
                // Slave server connection
                ObjectInputStream inHydrogen = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    try {
                        String received =  (String) inHydrogen.readObject();

                        //TODO: Optional - Maybe have a lsit of bonded and requested molecules
                        // TEMP: Displays updates from server
//                        synchronized (replyLock) {
//                            logs.add(received);
//                            oxygen.add(received);
//                            numOxygen++;
//                        }
                        System.out.println(received);

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

