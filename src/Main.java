import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        //Master or Slave
        System.out.println("Hydrogen(1), Oxygen(2) or Server(3)? ");
        Scanner scan = new Scanner(System.in);
        int choice = 0;

        while (choice != 1 && choice != 2 && choice != 3) {
            choice = scan.nextInt();
        }

        if (choice == 1) {
            // Start Hydrogen Client
            HydrogenClient h = new HydrogenClient();
            h.start();
        }
        else if (choice == 2) {
            // Start Oxygen Client
            OxygenClient o = new OxygenClient();
            o.start();
        }
        else {
            // Start Server
            Server server = new Server();
            server.start();
        }
    }
}