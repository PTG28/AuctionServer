package Auction.Client;

public class ClientResponseBuffer {
    public static final Object LOCK = new Object();
    public static String lastResponse = null;
}