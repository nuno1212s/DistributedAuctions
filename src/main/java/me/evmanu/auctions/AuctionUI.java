package me.evmanu.auctions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

// TODO: IMPLEMENTAR O MENU DO BIDDER

public class AuctionUI {

    public Scanner stdin = new Scanner(System.in);

    private int maxRows = 3;

    public AuctionUI() {

        int choice = mainMenu();

        switch(choice) {
            case 1:
                newAuctionMenu();
                break;
            case 2:
                ongoingAuction();
                break;
            case 3:
                System.exit(0);
                break;
            default:

        }
    }

    public int mainMenu() {

        System.out.print("1) New auction\n" +
                         "2) Find auctions\n" +
                         "3) Exit\n" +
                          "Choice: "
        );

        int choice = stdin.nextInt();

        while(choice <= 0 || choice > maxRows) {
            System.out.print("Bad option, choose again: ");
            choice = stdin.nextInt();
        }

        stdin.nextLine();

        return choice;
    }

    // OPTION 1
    public void newAuctionMenu() {
        System.out.println("** MENU TO INITIALIZE A AUCTION **\n");

        System.out.print("Name of item: ");
        String name = stdin.nextLine();

        System.out.print("Open bid: ");
        long openingBid = stdin.nextInt();

        System.out.print("Minimum bid: ");
        long minimumBid = stdin.nextInt();

        System.out.print("Auction duration (in min): ");
        long auctionDuration = stdin.nextInt();

        Auction newAuction = new Auction(name,openingBid,minimumBid,auctionDuration);
    }

    // OPTION 2
    public void ongoingAuction() {

        System.out.println("** LIST OF CURRENT AUCTIONS **\n");

        List<Auction> auctions = new ArrayList<Auction>();

        // TODO: buscar os leiloes na rede
        Auction newAuction = new Auction("La Ferrari",50000,80000,15); // apagar (para testes)
        auctions.add(newAuction); // apagar
        Auction newAuction2 = new Auction("Seat Ibiza",4500,3500,20); // apagar (para testes)
        auctions.add(newAuction2); // apagar

        int cont = 1;

        for (Auction auction : auctions) {
            System.out.println(cont + ") " + auction.getAuctionName());
            cont++;
        }

        System.out.print("\nEscolha: ");
        int choice = stdin.nextInt();

        while(choice <= 0 || choice >= cont) {
            System.out.print("Bad option, choose again: ");
            choice = stdin.nextInt();
        }

        stdin.nextLine();

        bidAuction(auctions.get(choice-1));
    }

    // method after entering on the 2 OPTION
    public void bidAuction(Auction chosenAuction) {
        System.out.println("** MENU TO BID THE ITEM **\n");

        System.out.println("Name of the item: " + chosenAuction.getAuctionName());
        System.out.println("Starting bid of the item: " + chosenAuction.getStartingBid());

        Date initialDate = new Date(chosenAuction.getInitialTs() * (long)1000);
        Date finalDate = new Date(chosenAuction.getFinalTs() * (long)1000);
        System.out.println("Start of auction: " + initialDate );
        System.out.println("End of auction: " + finalDate );

        System.out.print("\nPlease, choose a bid amount: ");

        float amount = stdin.nextFloat();

        while(amount <= chosenAuction.getStartingBid()) {
            System.out.print("Amount needs to be bigger than " + chosenAuction.getStartingBid() + ", choose again: ");
            amount = stdin.nextFloat();
        }

        stdin.nextLine();

        Bid currentBid = new Bid(amount);

        boolean b = currentBid.sendBid();

        if(b) {
            System.out.println("Bid was successfully submitted!");
        } else {
            System.out.println("Bid was not submitted!");
        }

    }



    public static void main(String Args[]) {

        AuctionUI auction = new AuctionUI();

    }
}
