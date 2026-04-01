package Auction.Server;

import Auction.model.Item;

import java.io.File;
import java.util.Scanner;


public class Services {

    public String requestAuction(String filename) {
        try {
            Item item = parseItemFromFile(filename);
            synchronized (ServerState.items) {
                ServerState.items.put(item.getId(), item);
            }
            return "Item stored with id: " + item.getId();

        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to store item: " + e.getMessage();
        }
    }

    public String listItems() {
        synchronized (ServerState.items) {
            if (ServerState.items.isEmpty())
                return "No items available";

            StringBuilder sb = new StringBuilder();

            for (Item item : ServerState.items.values()) {
                sb.append(item.getId()).append(". ").append(item.getName()).append("\n");
            }

            return sb.toString();
        }
    }

    public String listItemDetails(int itemId) {
        synchronized (ServerState.items) {

            if (ServerState.items.isEmpty())
                return "No available items";

            if (itemId < 0 || itemId >= ServerState.items.size())
                return "Invalid item number";

            Item item = (Item) ServerState.items.values().toArray()[itemId];

            return "ID: " + item.getId() +
                    "\nSeller: " + item.getSeller() +
                    "\nItem Name: " + item.getName() +
                    "\nDescription: " + item.getDescription() +
                    "\nStarting Price: " + item.getStartPrice() +
                    "\nCurrent Bid: " + item.getCurrentBid() +
                    "\nHighest Bidder: " + item.getHighestBidder() +
                    "\nAuction Time: " + item.getAuction_duration() + "\n";
        }
    }

    public String placeBid(int itemId, double amount, String bidder) {
        synchronized (ServerState.items) {

            if (!ServerState.items.containsKey(itemId))
                return "Item not found";

            Item item = ServerState.items.get(itemId);

            if (item.getSeller().equals(bidder))
                return "You cannot bid on your own item";

            if (amount <= item.getCurrentBid())
                return "Bid must be higher than current bid (" + item.getCurrentBid() + ")";

            item.setCurrentBid(amount);
            item.setHighestBidder(bidder);

            return "Bid placed successfully. New highest bid: " + amount;
        }
    }










    private Item parseItemFromFile(String filename) throws Exception {
        String seller = null;
        String name = null;
        String description = null;
        double startPrice = 0;
        int auction_duration = 0;

        Scanner fileScanner = new Scanner(new File(filename));

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

        fileScanner.close();

        return new Item(seller, name, description, startPrice, auction_duration);
    }

}// Services
