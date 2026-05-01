package Auction.Client;

import java.io.ObjectOutputStream;

public class AuctionPoller extends Thread {

    private final ObjectOutputStream out;
    private volatile boolean active = true;
    private String lastSeenAuction = "";

    public AuctionPoller(ObjectOutputStream out) {
        this.out = out;
    }

    @Override
    public void run() {
        while (active) {
            try {
                Thread.sleep(10000); // 10 sec for testing, can become 60000 later

                synchronized (out) {
                    out.writeUTF("GET_CURRENT_AUCTION");
                    out.flush();
                }

                String response = waitForPollResponse();

                if (response != null && !response.equals(lastSeenAuction)) {
                    lastSeenAuction = response;
                    System.out.println("\n[AUCTION UPDATE] " + response);
                }

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.out.println("Auction poller stopped.");
                break;
            }
        }
    }

    public void stopPolling() {
        active = false;
        this.interrupt();
    }

    private String waitForPollResponse() {
        synchronized (ClientResponseBuffer.LOCK) {
            while (ClientResponseBuffer.lastResponse == null) {
                try {
                    ClientResponseBuffer.LOCK.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }

            String response = ClientResponseBuffer.lastResponse;
            ClientResponseBuffer.lastResponse = null;
            return response;
        }
    }
}