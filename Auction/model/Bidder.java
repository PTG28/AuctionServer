package Auction.model;

public class Bidder extends User {
    private int id;
    private String name;
    private String surname;

    public Bidder(String name, String surname, String username, String password) {
        super(username,password);
        this.name = name;
        this.surname = surname;
    }

    public int getId() {return id;}

    public String getName() {return name;}

    public String getSurname() {return surname;}

    public void setId(int id) {this.id = id;}

    public void setName(String name) {this.name = name;}

    public void setSurname(String surname) {this.surname = surname;}

    @Override
    public String toString() {
        return "Bidder{" +
                "name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", username='" + getUsername() + '\'' +
                '}';
    }

 }//Bidder