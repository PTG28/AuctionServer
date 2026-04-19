package Auction.Server;

import Auction.model.Item;
import Auction.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ServerState {

    public static final Object AUCTION_LOCK = new Object();                     // lockarei 1 thread tin fora kanei allagh

    public static final Map<Integer, Item> items = new HashMap<>();
    public static final Map<String, User> users = new HashMap<>();

    public static List<ClientHandler> onlineClients = new ArrayList<>();
    public static Queue<Item> auctionQueue = new LinkedList<>();

    public static  Item currentItem = null;

    public static long currentAuctionEndTimeMillis = 0L;

    //public static volatile Item currentItem = null;                                     // otan allaxei timi to vlepoun ola ta threads apeutheias
    //public static volatile long currentAuctionEndTimeMillis = 0L;
}
