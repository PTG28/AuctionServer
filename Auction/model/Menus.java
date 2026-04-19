package Auction.model;

public class Menus {

    public static String guestMenu() {
        return "Welcome to Amesh Dimoprasia\n" +
                "===============================\n" +
                "1. Login\n" +
                "2. Register\n" +
                "0. Exit\n";
    }

    public static String bidderMenu() {
        return "\nBIDDER MENU\n" +
                "===========\n" +
                "1. Auction Item\n" +
                "2. Get Current Auction\n" +
                "3. Get Auction Details\n" +
                "4. Place Bid\n" +
                "5. Transaction Status\n" +
                "0. Logout\n";
    }

}// Menus
