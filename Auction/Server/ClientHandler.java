package Auction.Server;

import java.io.File;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import Auction.model.Item;


public class ClientHandler extends Thread {

    //Scanner scan = new Scanner(System.in);
    private Socket connection;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User loggedInUser = null;

    public ClientHandler(Socket connection) {
        this.connection = connection;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = in.readUTF();
                String response = handleMessage(msg);

                out.writeUTF(response);
                out.flush();

                if (msg.equals("5"))
                    break;
            }
        } catch (EOFException | SocketException e) {
            System.out.println("Client disconnected.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String handleMessage(String msg) {
        System.out.println("Message from client: " + msg);
        String[] parts = msg.split("\\|");
        String command = parts[0];
        switch (command) {
            case "1":
                if (parts.length < 2 || parts[1].isEmpty())
                    return ("Missing file name");
                return sellItem(parts[1].trim());

            case "2":
                return listItems();

            case "3":
                if (parts[1].isEmpty())
                    return ("Product index missing");
                return listItemDetails(Integer.parseInt(parts[1]));

            case "4":
                if (parts.length < 2)
                    return "Missing bid amount";
                return placeBid(parts[1]);

            case "5":
                return ("Thank you for your attendance! Hope to see you back soon");

            case "6":
                return getCurrentAuction();

            case "7":
                return getAuctionDetails();
            case "8":
                if (parts.length < 3)
                    return "Missing username or password";
                return handleRegister(parts[1], parts[2]);

            case "9":
                if (parts.length < 3)
                    return "Missing username or password";
                return handleLogin(parts[1], parts[2]);

            case "10":
                return handleLogout();

            default:
                return "Unknown command";

        }
    }

    private String sellItem(String filename) {

        if (loggedInUser == null) {
            return "You must login before selling an item";
        }

        String seller = null;
        String name = null;
        String description = null;
        double startPrice = 0;
        int auction_duration = 0;

        try {
            File itemfile = new File("items", filename);

            System.out.println("Trying file: " + itemfile.getAbsolutePath());

            if (!itemfile.exists()) {
                return "Error: file not found -> " + itemfile.getAbsolutePath();
            }

            try (Scanner fileScanner = new Scanner(itemfile)) {
                while (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine().trim();

                    if (line.isEmpty()) continue;

                    String[] parts = line.split(":", 2);
                    if (parts.length < 2) continue;

                    String key = parts[0].trim().toLowerCase();
                    String value = parts[1].trim();

                    switch (key) {
                        case "seller":
                            seller = value;
                            break;
                        case "item name":
                            name = value;
                            break;
                        case "description":
                            description = value;
                            break;
                        case "starting price":
                            startPrice = Double.parseDouble(value);
                            break;
                        case "auction time":
                            auction_duration = Integer.parseInt(value);
                            break;
                    }
                }
            }

            if (seller == null || name == null || description == null || startPrice <= 0 || auction_duration <= 0) {
                return "Error: invalid or incomplete item file data";
            }
            seller = loggedInUser.username;

            int id = Server.nextItemId++;
            Item item = new Item(id, seller, name, description, startPrice, auction_duration);

            ServerState.auctionQueue.add(item);

            return "Item added successfully to auction queue";

        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    private String listItems() {
        synchronized (ServerState.auctionQueue) {
            if (ServerState.auctionQueue.isEmpty())
                return "No items available";

            int counter = 0;
            StringBuilder sb = new StringBuilder();
            for (Item item : ServerState.auctionQueue) {
                sb.append(counter).append(". ").append(item.getName()).append("\n");
                counter++;
            }

            if (ServerState.auctionActive && ServerState.currentItem != null) {
                sb.append("\nCURRENT AUCTION: ").append(ServerState.currentItem.getName()).append("\n");
            }

            return sb.toString();
        }
    }

    private String listItemDetails(int index) {
        Object[] itemsArray = ServerState.auctionQueue.toArray();

        if (itemsArray.length == 0)
            return "No available items";

        if (index < 0 || index >= itemsArray.length)
            return "Invalid item number";

        Item item = (Item) itemsArray[index];

        return "ID: " + item.getId() +
                "\nSeller: " + item.getSeller() +
                "\nItem Name: " + item.getName() +
                "\nDescription: " + item.getDescription() +
                "\nStarting Price: " + item.getStartPrice() +
                "\nCurrent Bid: " + item.getCurrentBid() +
                "\nHighest Bidder: " + item.getHighestBidder() +
                "\nAuction Time: " + item.getAuction_duration();
    }

    // ================= METHODS =================

    private String handleRegister(String username, String password) {
        if (ServerState.users.containsKey(username)) {
            return "Username already exists";
        }

        ServerState.users.put(username, new User(username, password));
        return "Register successful";
    }

    private String handleLogin(String username, String password) {
        User user = ServerState.users.get(username);

        if (user != null && user.password.equals(password)) {
            loggedInUser = user;
            return "Login successful. Welcome " + username;
        } else {
            return "Login failed";
        }
    }
    private String handleLogout() {
        if (loggedInUser == null) {
            return "No user is currently logged in";
        }

        String username = loggedInUser.username;
        loggedInUser = null;
        return "Logout successful. Goodbye " + username;
    }

    private void handleAddItem(String[] parts) throws Exception {

        String objectId = parts[1];
        String desc = parts[2];
        double startBid = Double.parseDouble(parts[3]);
        int duration = Integer.parseInt(parts[4]);
        String seller = parts[5];

        Item item = new Item();

        Server.items.add(item);

        out.writeUTF("Item added to auction queue");
        out.flush();
    }


    private String placeBid(String bidAmountStr) {
        try {
            if (loggedInUser == null) {
                return "You must login before placing a bid";
            }

            if (!ServerState.auctionActive || ServerState.currentItem == null) {
                return "No active auction at the moment";
            }

            double bidAmount = Double.parseDouble(bidAmountStr);

            if (bidAmount <= 0) {
                return "Bid must be greater than 0";
            }

            synchronized (ServerState.currentItem) {
                if (!ServerState.auctionActive || ServerState.currentItem == null) {
                    return "No active auction at the moment";
                }

                if (System.currentTimeMillis() >= ServerState.auctionEndTime) {
                    return "Auction has already ended";
                }

                if (ServerState.currentItem.getSeller().equals(loggedInUser.username)) {
                    return "You cannot bid on your own item";
                }

                if (bidAmount <= ServerState.currentItem.getCurrentBid()) {
                    return "Bid rejected. Your bid must be higher than current bid: " +
                            ServerState.currentItem.getCurrentBid();
                }

                ServerState.currentItem.setCurrentBid(bidAmount);
                ServerState.currentItem.setHighestBidder(loggedInUser.username);

                return "Bid accepted! New highest bid: " + bidAmount +
                        " by " + loggedInUser.username;
            }

        } catch (NumberFormatException e) {
            return "Invalid bid amount";
        } catch (Exception e) {
            return "Error placing bid: " + e.getMessage();
        }
    }
    private String getCurrentAuction() {
        if (!ServerState.auctionActive || ServerState.currentItem == null) {
            return "No active auction at the moment";
        }

        Item item = ServerState.currentItem;

        return "Current auction item: " + item.getName() +
                "\nSeller: " + item.getSeller() +
                "\nDescription: " + item.getDescription();
    }
    private String getAuctionDetails() {
        if (!ServerState.auctionActive || ServerState.currentItem == null) {
            return "No active auction at the moment";
        }

        Item item = ServerState.currentItem;
        long remainingMillis = ServerState.auctionEndTime - System.currentTimeMillis();
        long remainingSeconds = Math.max(0, remainingMillis / 1000);

        return "Auction details:" +
                "\nItem: " + item.getName() +
                "\nCurrent bid: " + item.getCurrentBid() +
                "\nHighest bidder: " + item.getHighestBidder() +
                "\nTime left: " + remainingSeconds + " seconds";
    }
}