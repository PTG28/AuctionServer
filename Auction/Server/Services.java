package Auction.Server;

import Auction.model.Item;
import Auction.model.User;

import java.io.File;
import java.util.Scanner;

public class Services {


    private boolean isValidToken(String tokenId) {
        synchronized (ServerState.onlinePeersByToken) {
            return ServerState.onlinePeersByToken.containsKey(tokenId);
        }
    }

    private boolean isTokenOwnedByUser(String tokenId, String username) {
        synchronized (ServerState.onlinePeersByToken) {
            if (!ServerState.onlinePeersByToken.containsKey(tokenId)) {
                return false;
            }
            return ServerState.onlinePeersByToken.get(tokenId).getUsername().equals(username);
        }
    }

    public String requestAuction(String filename, String tokenId, String sellerUsername) {
        try {
            if (!isValidToken(tokenId)) {
                return "Invalid token";
            }

            if (!isTokenOwnedByUser(tokenId, sellerUsername)) {
                return "Token does not belong to current user";
            }

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

            String sellerToken = "UNKNOWN";
            synchronized (ServerState.usernameToToken) {
                if (ServerState.usernameToToken.containsKey(currentItem.getSeller())) {
                    sellerToken = ServerState.usernameToToken.get(currentItem.getSeller());
                }
            }

            return "item_id=" + currentItem.getId() +
                    " | seller_token=" + sellerToken +
                    " | seller_username=" + currentItem.getSeller() +
                    " | name=" + currentItem.getName() +
                    " | description=" + currentItem.getDescription() +
                    " | highest_bid=" + currentItem.getCurrentBid() +
                    " | highest_bidder=" + currentItem.getHighestBidder() +
                    " | remaining_time=" + remainingSeconds + " sec";
        }
    }

    public String placeBid(int itemId, double amount, String bidder, String tokenId) {
        if (!isValidToken(tokenId)) {
            return "Invalid token";
        }

        if (!isTokenOwnedByUser(tokenId, bidder)) {
            return "Token does not belong to current user";
        }

        String broadcastMsg;

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

            broadcastMsg =
                    "New highest bid for item " + item.getId() +
                            " (" + item.getName() + ")" +
                            " | bidder=" + bidder +
                            " | amount=" + amount;
        }

        broadcastMessage(broadcastMsg);
        return "Bid placed successfully. New highest bid: " + amount;
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
                                ". Contact seller " + item.getSeller() +
                                " and request metadata file: " + item.getMetadataFileName()
                );
            }

            if (seller != null) {
                seller.setPendingTransactionMessage(
                        "Your item " + item.getId() + " (" + item.getName() + ") was sold to " +
                                item.getHighestBidder() + " with bid " + item.getCurrentBid()
                );
            }
        }

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

        seller = sellerUsername;
        String metadataFileName = new File(filename).getName();
        return new Item(seller, name, description, startPrice, auctionDuration, metadataFileName);

    }

    public boolean isSellerActive(Item item) {
        if (item == null) {
            return false;
        }

        String sellerUsername = item.getSeller();
        String sellerToken;

        synchronized (ServerState.usernameToToken) {
            sellerToken = ServerState.usernameToToken.get(sellerUsername);
        }

        if (sellerToken == null) {
            return false;
        }

        synchronized (ServerState.onlinePeersByToken) {
            if (!ServerState.onlinePeersByToken.containsKey(sellerToken)) {
                return false;
            }

            String ip = ServerState.onlinePeersByToken.get(sellerToken).getIpAddress();
            int port = ServerState.onlinePeersByToken.get(sellerToken).getPort();

            Auction.Client.PeerConnector connector = new Auction.Client.PeerConnector();
            return connector.checkActive(ip, port);
        }
    }

    public void cancelAuction(Item item, String reason) {
        if (item == null) {
            return;
        }

        String notificationMessage;

        synchronized (ServerState.AUCTION_LOCK) {
            if (ServerState.currentItem == null || ServerState.currentItem.getId() != item.getId()) {
                return;
            }

            System.out.println("Auction cancelled for item " + item.getId() + ": " + reason);

            notificationMessage =
                    "Auction cancelled for item " + item.getId() +
                            " (" + item.getName() + ")" +
                            " | reason=" + reason;

            ServerState.currentItem = null;
            ServerState.currentAuctionEndTimeMillis = 0L;
        }

        broadcastMessage(notificationMessage);
    }
    public String getTransactionPeerInfo(String username) {
        synchronized (ServerState.items) {
            for (Item item : ServerState.items.values()) {
                if (item.getHighestBidder().equals(username)) {

                    synchronized (ServerState.AUCTION_LOCK) {
                        if (ServerState.currentItem != null && ServerState.currentItem.getId() == item.getId()) {
                            return "Auction is still running";
                        }
                    }

                    String sellerUsername = item.getSeller();

                    String sellerToken;
                    synchronized (ServerState.usernameToToken) {
                        sellerToken = ServerState.usernameToToken.get(sellerUsername);
                    }

                    if (sellerToken == null) {
                        return "Seller is offline";
                    }

                    synchronized (ServerState.onlinePeersByToken) {
                        if (!ServerState.onlinePeersByToken.containsKey(sellerToken)) {
                            return "Seller is offline";
                        }

                        String ip = ServerState.onlinePeersByToken.get(sellerToken).getIpAddress();
                        int port = ServerState.onlinePeersByToken.get(sellerToken).getPort();

                        return "TRANSACTION_INFO|" +
                                item.getId() + "|" +
                                sellerUsername + "|" +
                                ip + "|" +
                                port + "|" +
                                item.getMetadataFileName();
                    }
                }
            }
        }

        return "No pending transaction";
    }

    public String confirmTransfer(int itemId, String buyerUsername, String tokenId) {
        if (!isValidToken(tokenId)) {
            return "Invalid token";
        }

        if (!isTokenOwnedByUser(tokenId, buyerUsername)) {
            return "Token does not belong to current user";
        }

        synchronized (ServerState.items) {
            if (!ServerState.items.containsKey(itemId)) {
                return "Item not found";
            }

            Item item = ServerState.items.get(itemId);

            synchronized (ServerState.AUCTION_LOCK) {
                if (ServerState.currentItem != null && ServerState.currentItem.getId() == itemId) {
                    return "Auction is still running";
                }
            }

            if (!item.getHighestBidder().equals(buyerUsername)) {
                return "Only the winning bidder can confirm transfer";
            }

            item.setSeller(buyerUsername);
            ServerState.items.put(item.getId(), item);

            return "TRANSFER_CONFIRMED";
        }
    }

    public void broadcastMessage(String message) {
        synchronized (ServerState.onlineClients) {
            for (ClientHandler clientHandler : ServerState.onlineClients) {
                clientHandler.sendAsyncMessage(message);
            }
        }
    }

}
