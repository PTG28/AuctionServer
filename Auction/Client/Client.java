package Auction.Client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        System.out.println("Client Started");

        try (
                Socket server = new Socket("localhost", 8080);
                ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(server.getInputStream());
                Scanner scan = new Scanner(System.in)
        ) {
            printmenu();
            boolean running = true;
            while (running){
                String msg = scan.nextLine().trim();

                switch (msg){
                    case "1":
                        System.out.print("Please provide the name of the file which includes the item(s) for sale: ");
                        String filename = scan.nextLine().trim();
                        out.writeUTF("1|" + filename);
                        out.flush();

                        System.out.println(in.readUTF());
                        break;

                    case "2":
                        out.writeUTF("2");
                        out.flush();

                        System.out.println(in.readUTF());
                        break;

                    case "3":
                        System.out.print("Choose the item's index to list more details: ");
                        String index = scan.next().trim();
                        out.writeUTF("3|" + index);
                        out.flush();

                        System.out.println(in.readUTF());
                        break;

                    case "5":
                        out.writeUTF("5");
                        out.flush();

                        System.out.println(in.readUTF());
                        running = false;
                        break;

                    default:
                        System.out.println("Invalid option. Please try again.");
                }

                if(running == true)
                    System.out.print("Choose action: ");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void printmenu(){
        System.out.println("\nWelcome to Amesi Dimoprasia");
        System.out.println("=== MENU ===");
        System.out.println("1. Sell item");
        System.out.println("2. List items");
        System.out.println("3. List item details");

        System.out.println("5. Exit");
        System.out.print("Choose action: ");
    }

}