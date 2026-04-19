package Auction.Server;

import Auction.model.Item;
import Auction.model.User;

import java.io.File;
import java.util.Scanner;

public class Services {

    public String requestAuction(String filename, String sellerUsername) {
        try {
            Item item = parseItemFromFile(filename, sellerUsername);

            synchronized (ServerState.items) {
                ServerState.items.put(item.getId(), item);
            }
            synchronized (ServerState.auctionQueue) {
                ServerState.auctionQueue.add(item);
            }
            synchronized (ServerState.users) {
                User seller = ServerState.users.get(sellerUsername);
                if (seller != null) {
                    seller.incrementSellerAuctions();
                }
            }

            return "Item stored with id: " + item.getId() + " and added to auction queue";
        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to store item: " + e.getMessage();
        }
    }

    public String getCurrentAuction() {
        synchronized (ServerState.AUCTION_LOCK) {
            Item currentItem = ServerState.currentItem;

            if (currentItem == null) {
                return "No items available";
            }

            return "item_id=" + currentItem.getId() + " | name=" + currentItem.getName();
        }
    }

    public String getAuctionDetails() {
        synchronized (ServerState.AUCTION_LOCK) {
            Item currentItem = ServerState.currentItem;

            if (currentItem == null) {
                return "No active auction";
            }

            long remainingMillis = Math.max(0L, ServerState.currentAuctionEndTimeMillis - System.currentTimeMillis());
            long remainingSeconds = (remainingMillis + 999L) / 1000L;

            return "item_id=" + currentItem.getId() +
                    " | seller=" + currentItem.getSeller() +
                    " | name=" + currentItem.getName() +
                    " | description=" + currentItem.getDescription() +
                    " | highest_bid=" + currentItem.getCurrentBid() +
                    " | highest_bidder=" + currentItem.getHighestBidder() +
                    " | remaining_time=" + remainingSeconds + " sec";
        }
    }

    public String placeBid(int itemId, double amount, String bidder) {
        synchronized (ServerState.AUCTION_LOCK) {
            if (ServerState.currentItem == null) {
                return "No active auction";
            }

            if (ServerState.currentItem.getId() != itemId) {
                return "This item is not the current active auction";
            }

            if (System.currentTimeMillis() >= ServerState.currentAuctionEndTimeMillis) {
                return "Auction has already ended";
            }

            Item item = ServerState.currentItem;

            if (item.getSeller().equals(bidder)) {
                return "You cannot bid on your own item";
            }

            if (amount <= item.getCurrentBid()) {
                return "Bid must be higher than current bid (" + item.getCurrentBid() + ")";
            }

            item.setCurrentBid(amount);
            item.setHighestBidder(bidder);
            synchronized (ServerState.items) {
                ServerState.items.put(item.getId(), item);
            }

            return "Bid placed successfully. New highest bid: " + amount;
        }
    }

    public void completeAuctionTransaction(Item item) {
        if (item == null || item.getHighestBidder().equals("None")) {
            return;
        }

        synchronized (ServerState.users) {
            User seller = ServerState.users.get(item.getSeller());
            User bidder = ServerState.users.get(item.getHighestBidder());

            if (bidder != null) {
                bidder.incrementBidderWins();
                bidder.setPendingTransactionMessage(
                        "You won item " + item.getId() + " (" + item.getName() + ") with bid " + item.getCurrentBid() +
                                ". Metadata file: " + item.getMetadataFileName()
                );
            }

            if (seller != null) {
                seller.setPendingTransactionMessage(
                        "Your item " + item.getId() + " (" + item.getName() + ") was sold to " +
                                item.getHighestBidder() + " with bid " + item.getCurrentBid()
                );
            }
        }

        item.setSeller(item.getHighestBidder());
        synchronized (ServerState.items) {
            ServerState.items.put(item.getId(), item);
        }
    }

    public String getTransactionMessage(String username) {
        synchronized (ServerState.users) {
            User user = ServerState.users.get(username);
            if (user == null) {
                return "User not found";
            }
            return user.getPendingTransactionMessage() +
                    " | seller_count=" + user.getNumAuctionsSeller() +
                    " | bidder_wins=" + user.getNumAuctionsBidder();
        }
    }

    private Item parseItemFromFile(String filename, String sellerUsername) throws Exception {
        String seller = null;
        String name = null;
        String description = null;
        double startPrice = 0;
        int auctionDuration = 0;

        Scanner fileScanner = new Scanner(new File(filename));

        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(":", 2);
            if (parts.length < 2) {
                continue;
            }

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
                    auctionDuration = Integer.parseInt(value);
                    break;
                default:
                    break;
            }
        }

        fileScanner.close();

        if (seller == null || seller.isBlank()) {
            seller = sellerUsername;
        }

        return new Item(seller, name, description, startPrice, auctionDuration, filename);
    }
}
