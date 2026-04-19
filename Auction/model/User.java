package Auction.model;

public class User {
    String username;
    String password;
    private boolean isActive;
    private int numAuctionsSeller;
    private int numAuctionsBidder;
    private String pendingTransactionMessage;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.pendingTransactionMessage = "No completed transaction yet";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getNumAuctionsSeller() {
        return numAuctionsSeller;
    }

    public int getNumAuctionsBidder() {
        return numAuctionsBidder;
    }

    public String getPendingTransactionMessage() {
        return pendingTransactionMessage;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void incrementSellerAuctions() {
        this.numAuctionsSeller++;
    }

    public void incrementBidderWins() {
        this.numAuctionsBidder++;
    }

    public void setPendingTransactionMessage(String pendingTransactionMessage) {
        this.pendingTransactionMessage = pendingTransactionMessage;
    }
}
