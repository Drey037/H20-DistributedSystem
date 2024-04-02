import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hydrogen(1), Oxygen(2) or Server(3)? ");
        Scanner scan = new Scanner(System.in);
        int choice = scan.nextInt();

        switch (choice) {
            case 1:
                // Start Hydrogen Client
                HydrogenClient h = new HydrogenClient();
                h.start();
                h.shutdown();  // Shut down after start() completes
                break;
            case 2:
                // Start Oxygen Client
                OxygenClient o = new OxygenClient();
                o.start();
                // Assume OxygenClient has a similar shutdown method
                o.shutdown();  // Shut down after start() completes
                break;
            case 3:
                // Start Server
                Server server = new Server();
                server.start();  // Server likely runs indefinitely
                break;
            default:
                System.out.println("Invalid choice. Exiting.");
                break;
        }

        scan.close();
    }
}
