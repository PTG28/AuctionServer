package Auction.Client;

import Auction.model.Menus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        System.out.println("Client Started");
        String currentRole = "GUEST";

        try (
                Socket server = new Socket("localhost", 8080);
                ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(server.getInputStream());
                Scanner scan = new Scanner(System.in)
        ) {

            boolean running = true;
            while (running){

                if (currentRole.equals("GUEST")){
                    System.out.println(Menus.guestMenu());
                    System.out.print("Choose action: ");
                    String msg = scan.nextLine().trim();

                    switch (msg){
                        case "1":
                            // Call function to handle the credentials
                            String[] parts = handleLoginCredentials(scan, out, in);
                            String state = parts[0];

                            if (state.equals("LOGIN_SUCCESS")){
                                currentRole = "BIDDER";
                                System.out.println("Login Successful");
                            }
                            else{ System.out.println(parts[0]); }
                            break;

                        case "2":
                            // Call function to handle credential
                            String result = handleRegisterRequest(scan, out, in);
                            System.out.println(result);
                            break;

                        case "0":
                            out.writeUTF("EXIT");
                            out.flush();
                            System.out.println(in.readUTF());
                            running = false;
                            break;

                        default:
                            System.out.println("Invalid option. Please try again.");
                    }
                } else {
                    String result = handleBidderActions(scan, out, in);
                    if(result != null){
                        if(result.equals("LOGOUT_SUCCESS")){
                            currentRole = "GUEST";
                            System.out.println("Logged out successfully");
                        }else{
                            System.out.print(result);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//===========================================================================================================================================================================//




    public static String[] handleLoginCredentials(Scanner scan, ObjectOutputStream out, ObjectInputStream in){
        System.out.print("Username: ");
        String username = scan.nextLine().trim();
        System.out.print("Password: ");
        String password = scan.nextLine().trim();

        try {
            out.writeUTF("LOGIN|" + username + "|" + password);
            out.flush();

            String result = in.readUTF();
            String[] parts = result.split("\\|");
            return parts;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Take the register credentials and sends to the Client Handler to create a new player
    public static String handleRegisterRequest(Scanner scan, ObjectOutputStream out, ObjectInputStream in){
        String username;
        String password;
        System.out.print("Name: ");
        String name = scan.nextLine();

        System.out.print("Surname: ");
        String surname = scan.nextLine();

        System.out.print("Username: ");
        username = scan.nextLine();

        System.out.print("Password: ");
        password = scan.nextLine();

        try {
            out.writeUTF("REGISTER|" + name +"|" + surname + "|" + username + "|" + password);
            out.flush();
            return in.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String handleBidderActions(Scanner scan, ObjectOutputStream out, ObjectInputStream in) throws IOException {
        System.out.println(Menus.bidderMenu());
        System.out.print("Choose action: ");
        String choice = scan.nextLine().trim();

        switch (choice){

            case "1":
                System.out.print("Please provide the name of the file which includes the item(s) for auction: ");
                String filename = scan.nextLine().trim();
                out.writeUTF("REQUEST_AUCTION|" + filename);
                out.flush();
                return in.readUTF();

            case "2":
                out.writeUTF("LIST_ITEMS");
                out.flush();
                return in.readUTF();

            case "3":
                System.out.print("Choose the item's id to list more details: ");
                String id = scan.nextLine().trim();
                out.writeUTF("LIST_ITEM_DETAILS|" + id);
                out.flush();
                return in.readUTF();

            case "4":
                System.out.print("Item ID: ");
                String itemId = scan.nextLine().trim();

                System.out.print("Bid amount: ");
                String amount = scan.nextLine().trim();

                out.writeUTF("PLACE_BID|" + itemId + "|" + amount);
                out.flush();

                return in.readUTF();

            case "0":
                out.writeUTF("LOGOUT");
                out.flush();
                return in.readUTF();
        }
        return "Invalid input";
    }

}// Client