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
                "1. Auction item\n" +
                "2. List items\n" +
                "3. List item details\n" +
                "4. Place bid\n" +
                "0. Logout\n";
    }

}// Menus