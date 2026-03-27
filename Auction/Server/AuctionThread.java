package Auction.Server;

import Auction.model.Item;

public class AuctionThread extends Thread {

    @Override
    public void run() {
        while (true) {
            try {

                if (!ServerState.auctionActive && !ServerState.auctionQueue.isEmpty()) {
                    startNextAuction();
                }

                if (ServerState.auctionActive && System.currentTimeMillis() >= ServerState.auctionEndTime) {
                    finishCurrentAuction();
                }

                Thread.sleep(1000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startNextAuction() {
        Item nextItem = ServerState.auctionQueue.poll();

        if (nextItem == null) {
            return;
        }

        ServerState.currentItem = nextItem;
        ServerState.auctionActive = true;
        ServerState.auctionEndTime = System.currentTimeMillis() + (nextItem.getAuction_duration() * 1000L);

        System.out.println("====================================");
        System.out.println("Auction started for item: " + nextItem.getName());
        System.out.println("Seller: " + nextItem.getSeller());
        System.out.println("Starting price: " + nextItem.getStartPrice());
        System.out.println("Duration: " + nextItem.getAuction_duration() + " seconds");
        System.out.println("====================================");
    }

    private void finishCurrentAuction() {
        Item item = ServerState.currentItem;

        if (item == null) {
            ServerState.auctionActive = false;
            return;
        }

        synchronized (item) {
            System.out.println("====================================");
            System.out.println("Auction ended for item: " + item.getName());

            if (item.getHighestBidder().equals("None")) {
                System.out.println("No bids were placed.");
            } else {
                System.out.println("Winner: " + item.getHighestBidder());
                System.out.println("Final bid: " + item.getCurrentBid());
            }

            System.out.println("====================================");

            ServerState.currentItem = null;
            ServerState.auctionActive = false;
            ServerState.auctionEndTime = 0;
        }
    }
}