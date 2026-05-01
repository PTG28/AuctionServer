package Auction.Server;

import Auction.model.Bidder;
import Auction.model.OnlinePeerInfo;
import Auction.model.User;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;

public class ClientHandler extends Thread {

    private final Socket connection;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private User currentUser = null;
    private String currentTokenId = null;
    private int currentPeerPort = -1;

    private final Services services = new Services();

    public ClientHandler(Socket connection) {
        this.connection = connection;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = in.readUTF();
                String response = handleMessage(msg);
                out.writeUTF(response);
                out.flush();

                if (msg.equals("EXIT")) {
                    break;
                }
            }
        } catch (EOFException | SocketException e) {
            System.out.println("Client disconnected.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private String handleMessage(String msg) throws Exception {
        System.out.println("Message from client: " + msg);

        String[] parts = msg.split("\\|");
        String command = parts[0];

        switch (command) {
            case "LOGIN":
                if (parts.length < 4 || parts[1].isEmpty() || parts[2].isEmpty() || parts[3].isEmpty()) {
                    return "Missing login info";
                }
                return handleLogin(parts);

            case "REGISTER":
                if (parts.length < 5 || parts[1].isEmpty() || parts[2].isEmpty() || parts[3].isEmpty()) {
                    return "Missing register info";
                }
                return handleRegister(parts);

            case "LOGOUT":
                if (parts.length < 2 || parts[1].isEmpty()) {
                    return "Missing token";
                }
                return handleLogout(parts[1]);

            case "EXIT":
                logoutCurrentUser();
                return "Goodbye..! You may have missed a rare item today..";

            case "REQUEST_AUCTION":
                if (parts.length < 3 || parts[1].isEmpty() || parts[2].isEmpty()) {
                    return "Missing request auction info";
                }
                if (currentUser == null || currentTokenId == null) {
                    return "Please login first";
                }
                return services.requestAuction(parts[1], parts[2], currentUser.getUsername());

            case "GET_CURRENT_AUCTION":
                return services.getCurrentAuction();

            case "GET_AUCTION_DETAILS":
            case "GET_AUCTIONS_DETAILS":
                return services.getAuctionDetails();

            case "PLACE_BID":
                if (parts.length < 4) {
                    return "Missing bid info";
                }
                if (currentUser == null || currentTokenId == null) {
                    return "Please login first";
                }

                int itemId = Integer.parseInt(parts[1]);
                double amount = Double.parseDouble(parts[2]);
                String tokenId = parts[3];

                return services.placeBid(itemId, amount, currentUser.getUsername(), tokenId);

            case "TRANSACTION":
                if (currentUser == null || currentTokenId == null) {
                    return "Please login first";
                }
                return services.getTransactionMessage(currentUser.getUsername());

            case "GET_TRANSACTION_PEER_INFO":
                if (currentUser == null || currentTokenId == null) {
                    return "Please login first";
                }
                return services.getTransactionPeerInfo(currentUser.getUsername());

            case "CONFIRM_TRANSFER":
                if (parts.length < 3 || parts[1].isEmpty() || parts[2].isEmpty()) {
                    return "Missing transfer confirmation info";
                }
                if (currentUser == null || currentTokenId == null) {
                    return "Please login first";
                }

                int transferItemId = Integer.parseInt(parts[1]);
                String transferToken = parts[2];

                return services.confirmTransfer(transferItemId, currentUser.getUsername(), transferToken);

            default:
                return "Invalid input";
        }
    }

    private String handleLogin(String[] parts) {
        String username = parts[1];
        String password = parts[2];
        int peerPort = Integer.parseInt(parts[3]);

        synchronized (ServerState.users) {
            if (!ServerState.users.containsKey(username)) {
                return "Username does not exist";
            }

            User user = ServerState.users.get(username);

            if (!user.getPassword().equals(password)) {
                return "Wrong password";
            }

            if (user.isActive()) {
                return "User already logged in";
            }

            user.setActive(true);
            currentUser = user;
            currentPeerPort = peerPort;
            currentTokenId = generateTokenId();
        }

        String ipAddress = connection.getInetAddress().getHostAddress();
        OnlinePeerInfo info = new OnlinePeerInfo(currentTokenId, ipAddress, currentPeerPort, currentUser.getUsername());

        synchronized (ServerState.onlinePeersByToken) {
            ServerState.onlinePeersByToken.put(currentTokenId, info);
        }

        synchronized (ServerState.usernameToToken) {
            ServerState.usernameToToken.put(currentUser.getUsername(), currentTokenId);
        }

        synchronized (ServerState.onlineClients) {
            if (!ServerState.onlineClients.contains(this)) {
                ServerState.onlineClients.add(this);
            }
        }

        return "LOGIN_SUCCESS|" + currentTokenId;
    }

    private String handleRegister(String[] parts) {
        String name = parts[1];
        String surname = parts[2];
        String username = parts[3];
        String password = parts[4];

        synchronized (ServerState.users) {
            if (ServerState.users.containsKey(username)) {
                return "Username already exists";
            }

            Bidder bidder = new Bidder(name, surname, username, password);
            ServerState.users.put(username, bidder);

            return "REGISTER_SUCCESS";
        }
    }

    private String handleLogout(String tokenId) {
        if (currentTokenId == null || !currentTokenId.equals(tokenId)) {
            return "Invalid token";
        }

        logoutCurrentUser();
        return "LOGOUT_SUCCESS";
    }

    private void logoutCurrentUser() {
        if (currentUser != null) {
            currentUser.setActive(false);

            synchronized (ServerState.usernameToToken) {
                ServerState.usernameToToken.remove(currentUser.getUsername());
            }
        }

        if (currentTokenId != null) {
            synchronized (ServerState.onlinePeersByToken) {
                ServerState.onlinePeersByToken.remove(currentTokenId);
            }
        }

        currentUser = null;
        currentTokenId = null;
        currentPeerPort = -1;

        synchronized (ServerState.onlineClients) {
            ServerState.onlineClients.remove(this);
        }
    }

    private String generateTokenId() {
        return UUID.randomUUID().toString();
    }

    private void cleanup() {
        logoutCurrentUser();
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAsyncMessage(String message) {
        try {
            synchronized (out) {
                out.writeUTF("NOTIFICATION|" + message);
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Failed to send async message to client.");
        }
    }
}