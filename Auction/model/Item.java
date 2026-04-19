package Auction.model;

public class Item {
    private int id;
    private String seller;
    private String name;
    private String description;
    private double startPrice;
    private double currentBid;
    private String highestBidder;
    private int auction_duration;
    private String metadataFileName;
    private static int idCounter = 0;

    public Item() {
    }

    public Item(String seller, String name, String description, double startPrice, int auction_duration, String metadataFileName) {
        this.id = idCounter++;
        this.seller = seller;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.currentBid = startPrice;
        this.auction_duration = auction_duration;
        this.highestBidder = "None";
        this.metadataFileName = metadataFileName;
    }

    public int getId() { return id; }
    public String getSeller() { return seller; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartPrice() { return startPrice; }
    public int getAuction_duration() { return auction_duration; }
    public double getCurrentBid() { return currentBid; }
    public String getHighestBidder() { return highestBidder; }
    public String getMetadataFileName() { return metadataFileName; }

    public void setSeller(String seller) { this.seller = seller; }
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
                ", auction_duration=" + auction_duration +
                ", metadataFileName='" + metadataFileName + '\'' +
                '}';
    }
}
