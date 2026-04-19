package Auction.Client;

import Auction.model.Menus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class Client {

    private static final Random RAND = new Random();

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
            while (running) {

                if (currentRole.equals("GUEST")) {
                    System.out.println(Menus.guestMenu());
                    System.out.print("Choose action: ");
                    String msg = scan.nextLine().trim();

                    switch (msg) {
                        case "1":
                            String[] parts = handleLoginCredentials(scan, out, in);
                            String state = parts[0];

                            if (state.equals("LOGIN_SUCCESS")) {
                                currentRole = "BIDDER";
                                System.out.println("Login Successful");
                            } else {
                                System.out.println(parts[0]);
                            }
                            break;

                        case "2":
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
                    if (result != null) {
                        if (result.equals("LOGOUT_SUCCESS")) {
                            currentRole = "GUEST";
                            System.out.println("Logged out successfully");
                        } else {
                            System.out.println(result);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] handleLoginCredentials(Scanner scan, ObjectOutputStream out, ObjectInputStream in) {
        System.out.print("Username: ");
        String username = scan.nextLine().trim();
        System.out.print("Password: ");
        String password = scan.nextLine().trim();

        try {
            out.writeUTF("LOGIN|" + username + "|" + password);
            out.flush();

            String result = in.readUTF();
            return result.split("\\|");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String handleRegisterRequest(Scanner scan, ObjectOutputStream out, ObjectInputStream in) {
        System.out.print("Name: ");
        String name = scan.nextLine();

        System.out.print("Surname: ");
        String surname = scan.nextLine();

        System.out.print("Username: ");
        String username = scan.nextLine();

        System.out.print("Password: ");
        String password = scan.nextLine();

        try {
            out.writeUTF("REGISTER|" + name + "|" + surname + "|" + username + "|" + password);
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

        switch (choice) {

            case "1":
                System.out.print("Please provide the name of the file which includes the item(s) for auction: ");
                String filename = scan.nextLine().trim();
                out.writeUTF("REQUEST_AUCTION|" + filename);
                out.flush();
                return in.readUTF();

            case "2":
                out.writeUTF("GET_CURRENT_AUCTION");
                out.flush();
                return "Current auction: " + in.readUTF();

            case "3":
                if (!isInterestedInAuction()) {
                    return "Bidder is not interested in this auction right now (RAND > 0.60)";
                }

                out.writeUTF("GET_AUCTION_DETAILS");
                out.flush();
                return "Interested bidder fetched details: " + in.readUTF();

            case "4":
                return placeAutomaticBid(out, in);

            case "5":
                out.writeUTF("TRANSACTION");
                out.flush();
                return in.readUTF();

            case "0":
                out.writeUTF("LOGOUT");
                out.flush();
                return in.readUTF();
        }
        return "Invalid input";
    }

    private static boolean isInterestedInAuction() {
        return RAND.nextDouble() < 0.60;
    }

    private static String placeAutomaticBid(ObjectOutputStream out, ObjectInputStream in) throws IOException {
        out.writeUTF("GET_AUCTION_DETAILS");
        out.flush();
        String details = in.readUTF();

        if (details.startsWith("No active auction")) {
            return details;
        }

        int itemId = extractInt(details, "item_id=");
        double highestBid = extractDouble(details, "highest_bid=");
        double randomFactor = RAND.nextDouble();
        double newBid = roundTo2Decimals(highestBid * (1 + randomFactor / 10.0));

        out.writeUTF("PLACE_BID|" + itemId + "|" + newBid);
        out.flush();
        String placeBidResponse = in.readUTF();

        return "Generated bid for item " + itemId +
                " using NewBid=HighestBid*(1+RAND/10), RAND=" + String.format("%.4f", randomFactor) +
                ", highestBid=" + highestBid +
                ", newBid=" + newBid +
                " | " + placeBidResponse;
    }

    private static int extractInt(String message, String key) {
        String value = extractValue(message, key);
        return Integer.parseInt(value);
    }

    private static double extractDouble(String message, String key) {
        String value = extractValue(message, key);
        return Double.parseDouble(value);
    }

    private static String extractValue(String message, String key) {
        int start = message.indexOf(key);
        if (start < 0) {
            throw new IllegalArgumentException("Missing key: " + key);
        }

        start += key.length();
        int end = message.indexOf(" | ", start);
        if (end < 0) {
            end = message.length();
        }

        return message.substring(start, end).trim().replace(" sec", "");
    }

    private static double roundTo2Decimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
