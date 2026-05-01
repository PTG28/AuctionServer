package Auction.Client;

import Auction.model.Menus;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Client {

    private static final Random RAND = new Random();
    private static final Object REQUEST_LOCK = new Object();

    public static void main(String[] args) {
        System.out.println("Client Started");

        String currentRole = "GUEST";
        String sessionToken = null;
        String loggedInUsername = null;

        AuctionPoller auctionPoller = null;
        AutoAuctionSubmitter autoAuctionSubmitter = null;

        Scanner bootScanner = new Scanner(System.in);
        System.out.print("Give peer listening port: ");
        int peerPort = Integer.parseInt(bootScanner.nextLine().trim());

        System.out.print("Give shared directory path: ");
        String sharedDirectory = bootScanner.nextLine().trim();

        PeerServer peerServer = new PeerServer(peerPort, sharedDirectory);
        peerServer.setDaemon(true);
        peerServer.start();

        try (
                Socket server = new Socket("localhost", 8080);
                ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(server.getInputStream());
                Scanner scan = new Scanner(System.in)
        ) {
            ServerNotificationListener notificationListener = new ServerNotificationListener(in);
            notificationListener.setDaemon(true);
            notificationListener.start();

            boolean running = true;
            while (running) {

                if (currentRole.equals("GUEST")) {
                    System.out.println(Menus.guestMenu());
                    System.out.print("Choose action: ");
                    String msg = scan.nextLine().trim();

                    switch (msg) {
                        case "1": {
                            LoginAttempt loginAttempt = readLoginCredentials(scan);
                            String result = sendRequestAndWait(
                                    out,
                                    "LOGIN|" + loginAttempt.username + "|" + loginAttempt.password + "|" + peerPort
                            );
                            String[] parts = result.split("\\|");

                            if (parts.length >= 1 && parts[0].equals("LOGIN_SUCCESS")) {
                                currentRole = "BIDDER";
                                loggedInUsername = loginAttempt.username;

                                if (parts.length >= 2) {
                                    sessionToken = parts[1];
                                }

                                System.out.println("Login Successful");
                                System.out.println("Session token: " + sessionToken);

                                auctionPoller = new AuctionPoller(out);
                                auctionPoller.setDaemon(true);
                                auctionPoller.start();

                                autoAuctionSubmitter = new AutoAuctionSubmitter(out, sharedDirectory, sessionToken);
                                autoAuctionSubmitter.setDaemon(true);
                                autoAuctionSubmitter.start();

                            } else {
                                System.out.println(result);
                            }
                            break;
                        }

                        case "2": {
                            String result = handleRegisterRequest(scan, out);
                            System.out.println(result);
                            break;
                        }

                        case "0":
                            System.out.println(sendRequestAndWait(out, "EXIT"));
                            running = false;
                            break;

                        default:
                            System.out.println("Invalid option. Please try again.");
                    }
                } else {
                    String result = handleBidderActions(scan, out, sessionToken, sharedDirectory);

                    if (result != null) {
                        if (result.equals("LOGOUT_SUCCESS")) {
                            currentRole = "GUEST";
                            sessionToken = null;
                            loggedInUsername = null;

                            if (auctionPoller != null) {
                                auctionPoller.stopPolling();
                                auctionPoller = null;
                            }

                            if (autoAuctionSubmitter != null) {
                                autoAuctionSubmitter.stopSubmitting();
                                autoAuctionSubmitter = null;
                            }

                            System.out.println("Logged out successfully");
                        } else {
                            System.out.println(result);
                        }
                    }
                }
            }

            if (auctionPoller != null) {
                auctionPoller.stopPolling();
            }
            if (autoAuctionSubmitter != null) {
                autoAuctionSubmitter.stopSubmitting();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String handleRegisterRequest(Scanner scan, ObjectOutputStream out) {
        System.out.print("Name: ");
        String name = scan.nextLine();

        System.out.print("Surname: ");
        String surname = scan.nextLine();

        System.out.print("Username: ");
        String username = scan.nextLine();

        System.out.print("Password: ");
        String password = scan.nextLine();

        try {
            return sendRequestAndWait(out, "REGISTER|" + name + "|" + surname + "|" + username + "|" + password);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String handleBidderActions(
            Scanner scan,
            ObjectOutputStream out,
            String sessionToken,
            String sharedDirectory
    ) throws IOException {

        System.out.println(Menus.bidderMenu());
        System.out.print("Choose action: ");
        String choice = scan.nextLine().trim();

        switch (choice) {

            case "1": {
                System.out.print("Please provide the name of the file which includes the item for auction: ");
                String fileNameOnly = scan.nextLine().trim();

                if (fileNameOnly.isEmpty()) {
                    return "Filename cannot be empty";
                }

                File file = new File(sharedDirectory, fileNameOnly);
                if (!file.exists()) {
                    return "File does not exist in shared directory";
                }

                return sendRequestAndWait(out, "REQUEST_AUCTION|" + file.getPath() + "|" + sessionToken);
            }

            case "2":
                return "Current auction: " + sendRequestAndWait(out, "GET_CURRENT_AUCTION");

            case "3":
                if (!isInterestedInAuction()) {
                    return "Bidder is not interested in this auction right now (RAND > 0.60)";
                }

                return "Interested bidder fetched details: " + sendRequestAndWait(out, "GET_AUCTION_DETAILS");

            case "4":
                return placeAutomaticBid(out, sessionToken);

            case "5": {
                String infoResponse = sendRequestAndWait(out, "GET_TRANSACTION_PEER_INFO");
                String[] infoParts = infoResponse.split("\\|");

                if (infoParts.length >= 6 && "TRANSACTION_INFO".equals(infoParts[0])) {
                    int itemId = Integer.parseInt(infoParts[1]);
                    String sellerIp = infoParts[3];
                    int sellerPort = Integer.parseInt(infoParts[4]);
                    String metadataFileName = infoParts[5];

                    PeerConnector peerConnector = new PeerConnector();
                    String transferResult = peerConnector.requestMetadataFile(
                            sellerIp,
                            sellerPort,
                            metadataFileName,
                            sharedDirectory
                    );

                    if (transferResult.startsWith("Transaction completed successfully")) {
                        String confirmResult = sendRequestAndWait(
                                out,
                                "CONFIRM_TRANSFER|" + itemId + "|" + sessionToken
                        );
                        return transferResult + " | " + confirmResult;
                    }

                    return transferResult;
                }

                return infoResponse;
            }

            case "0":
                return sendRequestAndWait(out, "LOGOUT|" + sessionToken);
        }

        return "Invalid input";
    }

    private static boolean isInterestedInAuction() {
        return RAND.nextDouble() < 0.60;
    }

    private static String placeAutomaticBid(ObjectOutputStream out, String sessionToken) throws IOException {
        String details = sendRequestAndWait(out, "GET_AUCTION_DETAILS");

        if (details.startsWith("No active auction") || details.startsWith("Auction is still running")) {
            return details;
        }

        int itemId = extractInt(details, "item_id=");
        double highestBid = extractDouble(details, "highest_bid=");
        double randomFactor = RAND.nextDouble();
        double newBid = roundTo2Decimals(highestBid * (1 + randomFactor / 10.0));

        String placeBidResponse = sendRequestAndWait(
                out,
                "PLACE_BID|" + itemId + "|" + newBid + "|" + sessionToken
        );

        return "Generated bid for item " + itemId +
                " using NewBid=HighestBid*(1+RAND/10), RAND=" + String.format("%.4f", randomFactor) +
                ", highestBid=" + highestBid +
                ", newBid=" + newBid +
                " | " + placeBidResponse;
    }

    private static LoginAttempt readLoginCredentials(Scanner scan) {
        System.out.print("Username: ");
        String username = scan.nextLine().trim();

        System.out.print("Password: ");
        String password = scan.nextLine().trim();

        return new LoginAttempt(username, password);
    }

    private static int extractInt(String message, String key) {
        String value = extractValue(message, key);
        return Integer.parseInt(value);
    }

    private static double extractDouble(String message, String key) {
        String value = extractValue(message, key);
        return Double.parseDouble(value);
    }

    private static String extractValue(String message, String key) {
        int start = message.indexOf(key);
        if (start < 0) {
            throw new IllegalArgumentException("Missing key: " + key);
        }

        start += key.length();
        int end = message.indexOf(" | ", start);
        if (end < 0) {
            end = message.length();
        }

        return message.substring(start, end).trim().replace(" sec", "");
    }

    private static double roundTo2Decimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static String sendRequestAndWait(ObjectOutputStream out, String request) throws IOException {
        synchronized (REQUEST_LOCK) {
            synchronized (out) {
                out.writeUTF(request);
                out.flush();
            }
            return waitForServerResponse();
        }
    }

    public static String waitForServerResponse() {
        synchronized (ClientResponseBuffer.LOCK) {
            while (ClientResponseBuffer.lastResponse == null) {
                try {
                    ClientResponseBuffer.LOCK.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            String response = ClientResponseBuffer.lastResponse;
            ClientResponseBuffer.lastResponse = null;
            return response;
        }
    }

    private static class LoginAttempt {
        private final String username;
        private final String password;

        private LoginAttempt(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    private static class AutoAuctionSubmitter extends Thread {

        private final ObjectOutputStream out;
        private final String sharedDirectory;
        private final String sessionToken;
        private final Set<String> submittedFiles = new HashSet<>();
        private volatile boolean active = true;

        private AutoAuctionSubmitter(ObjectOutputStream out, String sharedDirectory, String sessionToken) {
            this.out = out;
            this.sharedDirectory = sharedDirectory;
            this.sessionToken = sessionToken;
        }

        @Override
        public void run() {
            while (active) {
                try {
                    Thread.sleep(5000);

                    File dir = new File(sharedDirectory);
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));

                    if (files == null) {
                        continue;
                    }

                    for (File file : files) {
                        if (!active) {
                            return;
                        }

                        String absolutePath = file.getAbsolutePath();
                        if (submittedFiles.contains(absolutePath)) {
                            continue;
                        }

                        String response = sendRequestAndWait(
                                out,
                                "REQUEST_AUCTION|" + file.getPath() + "|" + sessionToken
                        );

                        if (response.startsWith("Item stored with id:")) {
                            submittedFiles.add(absolutePath);
                            System.out.println("\n[AUTO REQUEST_AUCTION] " + file.getName() + " -> " + response);
                        }
                    }

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.out.println("Auto auction submitter stopped.");
                    break;
                }
            }
        }

        public void stopSubmitting() {
            active = false;
            this.interrupt();
        }
    }
}