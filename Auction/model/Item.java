package Auction.model;

public class Item {
    private int id;
    private String seller;
    private String name;
    private String description;
    private double startPrice;
    private double currentBid;
    private String highestBidder;

    public Item() {
    }

    public Item(int id, String seller, String name, String description, double startPrice) {
        this.id = id;
        this.seller = seller;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.currentBid = startPrice;
        this.highestBidder = "None";
    }

    public int getId() { return id; }
    public String getSeller() { return seller; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartPrice() { return startPrice; }
    public double getCurrentBid() { return currentBid; }
    public String getHighestBidder() { return highestBidder; }

    public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }
    public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }

    @Override
    public String toString() {
        return "Item{id=" + id +
                ", seller='" + seller + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", startPrice=" + startPrice +
                ", currentBid=" + currentBid +
                ", highestBidder='" + highestBidder + '\'' +
                '}';
    }
}