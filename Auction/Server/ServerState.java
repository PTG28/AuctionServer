package Auction.Server;

import Auction.model.Item;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerState {

    public static ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    public static ConcurrentLinkedQueue<Item> auctionQueue = new ConcurrentLinkedQueue<>();

}