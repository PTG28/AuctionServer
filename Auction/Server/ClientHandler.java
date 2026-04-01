package Auction.Server;

import Auction.model.Bidder;
import Auction.model.User;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

import static Auction.Server.Server.items;


public class ClientHandler extends Thread {
    
    private final Socket connection;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User currentUser = null;

    private final Services services = new Services();

    public ClientHandler(Socket connection) throws Exception {
        this.connection = connection;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());

//            synchronized (ServerState.onlineClients) {
//                ServerState.onlineClients.add(this);                              // kathe Client pou anoigei mpainei sto list me tous online clients
//            }
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

                if (msg.equals("EXIT"))
                    break;
            }
        } catch (EOFException | SocketException e) {
            System.out.println("Client disconnected.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // =========== METHODS  ===========
    private String handleMessage(String msg) throws Exception {
        System.out.println("Message from client: " + msg);

        String[] parts = msg.split("\\|");
        String command = parts[0];

        switch (command) {
            case "LOGIN":
                if (parts.length < 3 || parts[1].isEmpty() || parts[2].isEmpty())
                    return ("Missing login info");
                return handleLogin(parts);

            case "REGISTER":
                if (parts.length < 5 || parts[1].isEmpty() || parts[2].isEmpty() || parts[3].isEmpty())
                    return "Missing register info";
                return handleRegister(parts);

            case "LOGOUT":
//                if(currentUser != null){
//                    currentUser.setActive(false);
//                    currentUser = null;
//                }
                synchronized (ServerState.onlineClients) {
                    ServerState.onlineClients.remove(this);
                }
                return "LOGOUT_SUCCESS";

            case "EXIT":
                return "Goodbye..! You may have missed a rare item today..";

//-------------------------------------------------------------------------------------------------//

            case "REQUEST_AUCTION":
                if(parts.length < 2 || parts[1].isEmpty())
                    return "Missing filename";
                return services.requestAuction(parts[1]);

            case "LIST_ITEMS":
                return services.listItems();

            case "LIST_ITEM_DETAILS":
                return services.listItemDetails(Integer.parseInt(parts[1]));

            case "PLACE_BID":
                if (parts.length < 3)
                    return "Missing bid info";

                int itemId = Integer.parseInt(parts[1]);
                double amount = Double.parseDouble(parts[2]);

                return services.placeBid(itemId, amount, currentUser.getUsername());


            default:
                return "Invalid input";
        }
    }

    // *** IMPORTANT *** ADD SYNCHRONISED METHOD WHEN USING THE HASHMAPS
    private String handleLogin(String[] parts) {
        String username = parts[1];
        String password = parts[2];

        synchronized (ServerState.users) {

            if (!ServerState.users.containsKey(username))
                return "Username does not exist";

            User user = ServerState.users.get(username);

            if (!user.getPassword().equals(password))
                return "Wrong password";

            currentUser = user;
            //currentUser.setActive(true);            // na fugw auto?
            synchronized (ServerState.onlineClients) {
                ServerState.onlineClients.add(this);
            }
            return "LOGIN_SUCCESS";
        }
    }

    private String handleRegister(String[] parts) throws Exception {
        String name = parts[1];
        String surname = parts[2];
        String username = parts[3];
        String password = parts[4];

        synchronized (ServerState.users) {

            if (ServerState.users.containsKey(username))
                return "Username already exists";

            Bidder bidder = new Bidder(name, surname, username, password);
            ServerState.users.put(username, bidder);

            return "REGISTER_SUCCESS";
        }
    }
}