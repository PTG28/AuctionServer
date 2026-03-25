package Auction.Server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerState {

    public static ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    public static ConcurrentLinkedQueue<AuctionItem> auctionQueue = new ConcurrentLinkedQueue<>();

}