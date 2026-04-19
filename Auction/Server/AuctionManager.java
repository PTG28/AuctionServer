package Auction.Server;

import Auction.model.Item;

public class AuctionManager extends Thread {
    private final Services services = new Services();

    @Override
    public void run() {
        while (true) {
            try {

                boolean auctionRunning;
                synchronized (ServerState.AUCTION_LOCK) {
                    auctionRunning = (ServerState.currentItem != null);
                }

                if (auctionRunning) {
                    Thread.sleep(500);
                    continue;
                }

                Item item;
                synchronized (ServerState.auctionQueue) {
                    item = ServerState.auctionQueue.poll();
                }
                if (item == null) {
                    Thread.sleep(1000);
                    continue;
                }

                startAuction(item);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startAuction(Item item) throws InterruptedException {
        synchronized (ServerState.AUCTION_LOCK) {
            ServerState.currentItem = item;
            ServerState.currentAuctionEndTimeMillis = System.currentTimeMillis() + (item.getAuction_duration() * 1000L);
        }

        System.out.println("Starting auction for: " + item.getName() +
                " | Item ID: " + item.getId() +
                " | Duration: " + item.getAuction_duration() + " sec");

        while (true) {
            Thread.sleep(1000);

            long remainingMillis;
            synchronized (ServerState.AUCTION_LOCK) {
                if (ServerState.currentItem == null || ServerState.currentItem.getId() != item.getId()) {
                    return;
                }
                remainingMillis = ServerState.currentAuctionEndTimeMillis - System.currentTimeMillis();
            }

            long remainingSeconds = Math.max(0L, (remainingMillis + 999L) / 1000L);
            System.out.println("Time left: " + remainingSeconds + " sec | Current bid: " + item.getCurrentBid());

            if (remainingMillis <= 0L) {
                break;
            }
        }

        endAuction(item);
    }

    private void endAuction(Item item) {
        synchronized (ServerState.AUCTION_LOCK) {
            if (ServerState.currentItem == null || ServerState.currentItem.getId() != item.getId()) {
                return;
            }

            System.out.println("Auction ended!");

            if (item.getHighestBidder().equals("None")) {
                System.out.println("No bids placed.");
            } else {
                System.out.println("Winner: " + item.getHighestBidder() + " with " + item.getCurrentBid());
                services.completeAuctionTransaction(item);
            }

            ServerState.currentItem = null;
            ServerState.currentAuctionEndTimeMillis = 0L;
        }
    }
}
