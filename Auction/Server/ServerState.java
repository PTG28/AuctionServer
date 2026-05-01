package Auction.Server;

import Auction.model.Item;
import Auction.model.OnlinePeerInfo;
import Auction.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ServerState {

    public static final Object AUCTION_LOCK = new Object();

    public static final Map<Integer, Item> items = new HashMap<>();
    public static final Map<String, User> users = new HashMap<>();

    public static final Map<String, OnlinePeerInfo> onlinePeersByToken = new HashMap<>();
    public static final Map<String, String> usernameToToken = new HashMap<>();

    public static final List<ClientHandler> onlineClients = new ArrayList<>();
    public static final Queue<Item> auctionQueue = new LinkedList<>();

    public static Item currentItem = null;
    public static long currentAuctionEndTimeMillis = 0L;
}