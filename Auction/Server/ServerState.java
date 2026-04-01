package Auction.Server;

import Auction.model.Item;
import Auction.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerState {

    public final static Map<Integer, Item> items = new HashMap<>();
    public final static Map<String, User> users = new HashMap<>();

    public static List<ClientHandler> onlineClients = new ArrayList<>();
    public  static Map<Integer, Item> auctionQueue = new HashMap<>();
}