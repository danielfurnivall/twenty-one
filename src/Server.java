import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    HashMap<String, clientThread> playerList = new HashMap<>();
    HashMap<String, clientThread> pendingList = new HashMap<>();
    ArrayList<String> naturals = new ArrayList<>();
    HashMap<String, Integer> scoreList = new HashMap<>();
    boolean inProgress = false; //to deal with mid-game joiners
    int connectioncounter = 0;
    int initialStakes;
    int tokensPerDeal;
    int numOfPlayers;



    public class logicThread extends Thread{ //where most of the logic occurs

        boolean naturalTwentyOnebool = false;
        int pot = 0;
        String dealer;

        public void sendCard(Card sentCard, String recipient, ArrayList<Card> cardDeck){
            playerList.get(recipient).hand.add(sentCard);
            cardDeck.remove(sentCard);
            System.out.println("Card dealt to "+recipient+": "+sentCard.toString());
        }

        public void massPrint(String printData) throws IOException{
            for(String key : playerList.keySet()){
                playerList.get(key).clientPrint(printData);
            }
        }
        public void nonPlayerPrint(String printdata) throws IOException{
            for(String key: pendingList.keySet()){
                pendingList.get(key).clientPrint(printdata);
            }
        }



        public void stakeRemaining() throws IOException{
            for(String key: playerList.keySet()){
                clientThread thisClient = playerList.get(key);
                thisClient.clientPrint("Your remaining stake is "+thisClient.getStake()+" tokens");
            }
        }

        public void initialTurnOrder() throws IOException{
            Random r = new Random();
            HashMap<Integer, String> randomDraw = new HashMap<Integer, String>();
            for(String key: pendingList.keySet()){
                randomDraw.put(r.nextInt(10), key);
            }
            ArrayList<Integer> randoms = new ArrayList<Integer>(randomDraw.keySet());
            int maxDraw = Collections.max(randoms);
            String winner = randomDraw.get(maxDraw);
            System.out.println(Collections.singletonList(randomDraw));
            changeTurnOrder(winner, pendingList.get(winner).turnOrder, pendingList);
        }


        public ArrayList<String> winnerList(HashMap<String, Integer> players){
            //iterate through scorelist to get list of winners
            ArrayList<String> winners = new ArrayList<>();
            int highscore = 0;
            for (Map.Entry<String, Integer> user : scoreList.entrySet()) {
                String key = user.getKey();
                Integer value = user.getValue();
                if (value > 21){
                    continue;
                }
                if (value > highscore) {
                    winners.clear();
                    winners.add(key);
                    highscore = value;
                }
                else if (value == highscore) {
                    winners.add(key);
                }
            }
            return winners;
        }
        public void dealerWins(ArrayList<String> winners) throws IOException {
            //make list of non winners
            ArrayList<String> otherPlayers = new ArrayList<>(playerList.keySet());
            otherPlayers.removeAll(winners);


            //tell players they didn't win and pile up their cash
            for (String player : otherPlayers) {
                clientThread playerThread = playerList.get(player);
                playerList.get(player).clientPrint("You did not win this round." +
                        "You have paid the dealer double stakes -" + tokensPerDeal*2 + "tokens.");
                playerThread.stake -= tokensPerDeal * 2;
                pot += tokensPerDeal  * 2;
            }
            if (winners.size() > 1) {
                massPrint(winners.size() + " players won, including dealer " + dealer);

                //let other winners know they didn't win or lose
                winners.remove(dealer);
                for (String winner : winners) {
                    clientThread winnerThread = playerList.get(winner);
                    winnerThread.clientPrint("You won, but so did the dealer. " +
                            "You haven't lost or gained anything.");
                }

                //give dealer the rest of the pot
                clientThread dealerThread = playerList.get(dealer);
                dealerThread.stake += pot;
                dealerThread.clientPrint("You won! You've gained " + pot + " tokens");
            }else {
                massPrint("Dealer was the winner.");
                //code for dealer being the only winner goes here
                clientThread dealerThread = playerList.get(dealer);
                dealerThread.stake += pot;
                dealerThread.clientPrint("You won! You've gained " + pot + " tokens");
            }
        }


        public void dealerLoses(ArrayList<String> winners) throws IOException{
            massPrint(scoreList.toString());
            int dealerScore = scoreList.get(dealer);
            clientThread dealerThread = playerList.get(dealer);
            scoreList.remove(dealer);

            if(naturalTwentyOnebool) {
                ArrayList<String> nonWinners = new ArrayList<>(playerList.keySet());
                nonWinners.removeAll(winners);
                for(String key: nonWinners){
                    clientThread playerThread = playerList.get(key);
                    playerThread.stake -= (tokensPerDeal*2);
                    pot += (tokensPerDeal*2);
                }
                if (winners.size() > 1) {
                    String trueWinner = winners.get(0);
                    winners.remove(trueWinner);
                    for (String winner : winners) {
                        clientThread winnerThread = playerList.get(winner);
                        winnerThread.stake += tokensPerDeal;
                        winnerThread.clientPrint("You won, but you did not have positional priority. " +
                                "You haven't lost or gained anything.");
                    }
                    clientThread winnerThread = playerList.get(trueWinner);

                    winnerThread.stake += pot;
                    winnerThread.clientPrint("You won the double stakes from everyone(" + pot + ")." +
                            "\nYou are now the dealer.");
                }
                else{
                    clientThread winnerThread = playerList.get(winners.get(0));
                    winnerThread.clientPrint("You won!");
                    winnerThread.stake += pot;
                }
                changeTurnOrder(winners.get(0), playerList.get(winners.get(0)).turnOrder,playerList);
            }
            else{
                for(String key: scoreList.keySet()){
                    int playerScore = scoreList.get(key);
                    clientThread playerThread = playerList.get(key);
                    if (dealerScore < 22){
                        if (playerScore < 22 && playerScore > dealerScore){
                            playerThread.stake += tokensPerDeal;
                            dealerThread.stake -= tokensPerDeal;

                            playerThread.clientPrint("You scored higher than the dealer. They" +
                                    " have paid you single stakes.");
                            dealerThread.clientPrint("Gave "+tokensPerDeal+" tokens to "+key);

                        }

                        else if (playerScore == dealerScore){
                            playerThread.clientPrint("You got the same score as the dealer. " +
                                    "Nothing lost, nothing gained.");


                        }
                        else if (playerScore < dealerScore){
                            playerThread.stake -= tokensPerDeal;
                            dealerThread.stake += tokensPerDeal;
                            playerThread.clientPrint("You scored less than the dealer," +
                                    " and have given them single stakes ("+(tokensPerDeal)+").");
                            dealerThread.clientPrint("You gained "+tokensPerDeal+" tokens from "+key);


                        }
                        else{
                            playerThread.stake -= tokensPerDeal;
                            dealerThread.stake += tokensPerDeal;
                            playerThread.clientPrint("You went bust and the dealer did not. You have paid" +
                                    " single stakes.");
                            dealerThread.clientPrint("You gained "+tokensPerDeal+" tokens from "+key);
                        }

                    }
                    else{

                        dealerThread.clientPrint("You went bust!");
                        if (playerScore > 22){
                            playerThread.clientPrint("Dealer went bust, but so did you! Nothing lost.");

                        }
                        if (playerScore < 22){
                            playerThread.stake += tokensPerDeal;
                            dealerThread.stake -= tokensPerDeal;
                            playerThread.clientPrint("You beat the dealer and gained" +
                                    " single stakes("+tokensPerDeal+").");
                            dealerThread.clientPrint("You gave "+tokensPerDeal+" to "+key);

                        }
                    }

                }
            }
            dealerThread.clientPrint("Your stake after that hand is "+dealerThread.stake);


        }


        public void settleScores() throws IOException {
            int highScore = 0;
            String[] order = getTurnOrderList(playerList);
            dealer = order[0];
            for (String key : order) {

                clientThread thisClient = playerList.get(key);
                int clientScore = thisClient.handScore();
                scoreList.put(key, clientScore);
            }

            massPrint("Final scores are as follows: " + scoreList+"\n");
            ArrayList<String> winners = winnerList(scoreList);

            if (naturalTwentyOnebool && winners.contains(dealer)){
                dealerWins(winners);
            }
            else if (winners.size() == 0){

                massPrint("No winners this round. Hopefully next round!\n");
            }
            else {
                //deal with situations where dealer doesn't get a natural twenty one
                dealerLoses(winners);
            }



            stakeRemaining();
        }

        public void assignStakes() throws IOException {
            pot = 0;
            for(String key: playerList.keySet()){
                clientThread thisClient = playerList.get(key);
                thisClient.stake = thisClient.stake - tokensPerDeal;
                pot += tokensPerDeal;
            }
            massPrint("\nTotal in the pot: "+pot);
        }

        public void naturalTwentyOne(){
            for (String key : playerList.keySet()){
                clientThread thisClient = playerList.get(key);
                if (thisClient.handScore() != 21){
                    try{ thisClient.clientPrint("You haven't won this round. Better luck next time!");

                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
                else{
                    try{
                        thisClient.clientPrint("You have a natural twenty one");

                    }catch(IOException e){
                        e.printStackTrace();
                    }


                }
            }
        }
        public void subsequentDeals(ArrayList<Card> cardDeck) throws IOException {
            Random r = new Random();
            int firstPlayerCatcher = 0;
            String[] order = getTurnOrderList(playerList);
            for (String key : order) {
                firstPlayerCatcher++;
                clientThread thisClient = playerList.get(key);
                thisClient.clientPrint("\nYour turn!\n");
                ArrayList<String> otherPlayers = new ArrayList<>(playerList.keySet());
                if (firstPlayerCatcher == 1){
                    otherPlayers.remove(key);
                    for(String player: otherPlayers){
                        clientThread thisPlayer = playerList.get(player);
                        thisPlayer.clientPrint("Other players are making their moves." +
                                " Please wait for your turn.");
                    }
                }

                while(thisClient.handScore()<22){
                    Card dealCard = cardDeck.get(r.nextInt(cardDeck.size()));

                    BufferedReader reader = new BufferedReader(new InputStreamReader(thisClient.client.getInputStream()));
                    String line;
                    thisClient.clientPrint("\nStick (s) or draw new card (d)?");
                    line = reader.readLine();
                    if (line.equals("s")){

                        break;
                    }
                    if (line.equals("d")){
                        sendCard(dealCard, key, cardDeck);
                        thisClient.clientPrint("New card drawn: "+dealCard.toString());

                    }
                }
                thisClient.clientPrint("\nFinal Score = "+thisClient.handScore()+"\nHand over - please wait for other players.\n");
            }
            try {
                settleScores();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        public String[] getTurnOrderList(HashMap<String, clientThread> list){
            String[] orderList = new String[(list.size())];
            for(String key: list.keySet()){
                clientThread thisClient = list.get(key);
                orderList[thisClient.turnOrder - 1] = key;
            }

            return orderList;
        }

        public void changeTurnOrder(String winner, int winnerCurrentPos, HashMap<String, clientThread> list) throws IOException{
            String[] currentOrder = getTurnOrderList(list);
            String[] tempArray = new String[currentOrder.length];

            for(int i = 0; i < currentOrder.length - winnerCurrentPos + 1; i++){
                tempArray[i] = currentOrder[winnerCurrentPos + i - 1];
            }

            int j = 0;
            for(int i=currentOrder.length - winnerCurrentPos +1; i < currentOrder.length; i++){
                tempArray[i] = currentOrder[j];
                j++;
            }

            int i = 1;


            for(String item: tempArray){
                list.get(item).turnOrder = i;
                System.out.println("Player "+item+": "+list.get(item).turnOrder);
                i++;
            }
            dealer = tempArray[0];
            for(String key: list.keySet()){
                list.get(key).clientPrint("\n A new dealer has been selected: "+dealer);
            }

        }
        public void initialDeal(ArrayList<Card> cardDeck) throws IOException{
            Random r = new Random();
            pot=0;
            String[] order = getTurnOrderList(playerList);
            int j = 0;
            for (String i: order){
                if (j == 0){
                    playerList.get(i).clientPrint("\nYou are the dealer.\n");
                }
                System.out.println(i);
                j++;
            }

            for (int i = 0; i < 2; i++) {
                for (String key : order) {
                    clientThread thisClient = playerList.get(key);


                    System.out.println(key + "'s turn.");
                    Card dealCard = cardDeck.get(r.nextInt(cardDeck.size()));

                    try {
                        sendCard(dealCard, key, cardDeck);
                        if (i == 1){
                            thisClient.clientPrint(thisClient.listHand());
                            int thisScore = thisClient.handScore();
                            if (thisScore == 21){
                                naturalTwentyOnebool =true;
                                massPrint("Player "+key+" scored a natural 21!");
                                naturals.add(key);
                                //if natural 21 happens, it must be immediately declared to all players.
                            }
                            System.out.println(thisScore);
//                            thisClient.clientPrint("Current hand score: "+thisScore);
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (naturalTwentyOnebool){
                settleScores();
            }
        }
        int counter = 0;
        public ArrayList<Card> buildShuffledDeck(){
            ArrayList<Card> newDeck = new ArrayList<>(52);
            for (int i = 1; i < 14; i++){
                for (int j = 0; j < 4; j++){
                    newDeck.add(new Card(i, j));
                }
            }
            counter ++;
            System.out.println(counter);
            System.out.println("Shuffling deck...");
            Collections.shuffle(newDeck);
            return newDeck;
        }

        public void wipeHandsandScores() throws IOException{
            for(String key: playerList.keySet()){
                clientThread thisClient = playerList.get(key);
                thisClient.hand = new ArrayList<Card>(2);
            }
            scoreList.clear();
            naturalTwentyOnebool = false;
            massPrint("\nNext hand starting in 5 seconds.\n\n\n");
        }

        public void kickPlayers() throws IOException, InterruptedException{
            boolean dealerleft = false;
            boolean leavers = false;
            ArrayList<String> stayList = new ArrayList<>(playerList.keySet());
            ArrayList<String> kickList = new ArrayList<>();

            for (String key : playerList.keySet()) {
                clientThread thisClient = playerList.get(key);

                if (thisClient.stake < tokensPerDeal * 2) {
                        thisClient.clientPrint("Sorry, you no longer have enough tokens to play (i.e. your tokens" +
                                " are less than double stakes value)\nYou may now close this window.");
                        thisClient.interrupt();
                    kickList.add(key);
                    leavers=true;
                    if (thisClient.turnOrder == 1){
                        dealerleft = true;
                        kickList.add(key);

                    }
                }


            }


            stayList.removeAll(kickList);
            int i = 1;
            if (leavers){
                for(String key: stayList){
                    playerList.get(key).turnOrder=i;
                    i++;
                }
            }

            for(String client : kickList){
                playerList.remove(client);
            }
            if (dealerleft){
                massPrint("\nDealer has left the game. New dealer: "+stayList.get(0));
            }

        }



        public void run() {




            while (pendingList.size() < numOfPlayers) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("error");
                }
            }
            try {
                nonPlayerPrint("Players found - 10 seconds remaining to join the game!");
                Thread.sleep(10000);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
            System.out.println("------Players found - Game Starting------");
            try{
                initialTurnOrder();
                for(String key:pendingList.keySet()){
                    pendingList.get(key).clientPrint("The initial dealer has been selected.");
                }
            }catch(IOException e){
                e.printStackTrace();
            }

            while (true) {
                //get list of players

                if (playerList.size() + pendingList.size() == 1){
                    break;
                }
                try{
                    playerList.putAll(pendingList);
                    pendingList.clear();
                }catch(NullPointerException e){
                    System.out.println("No players waiting to join :)");
                }
                inProgress=true;
                System.out.println("Players for this round are " + playerList.keySet());


                System.out.println("Building deck...");


                ArrayList<Card> mainDeck = buildShuffledDeck();
                try {
                    initialDeal(mainDeck);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Deck remaining: " + mainDeck.size());
                if (naturalTwentyOnebool) {
                    try {
                        massPrint("We have a twenty one!");
                        naturalTwentyOne();
                        //code to deal
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        massPrint("No natural twenty ones - let's keep playing.\n");
                        subsequentDeals(mainDeck);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    kickPlayers();
                    wipeHandsandScores();
                    inProgress=false;
                    Thread.sleep(10000);
                }catch (IOException | InterruptedException e){
                    e.printStackTrace();
                }
            }
            try{
                massPrint("You are the only remaining player, and you have all the tokens. You're the best!" +
                        "\nYou may now close this window.");
            }catch(IOException e){
                e.printStackTrace();
            }

        }
    }


    public class clientThread extends Thread{
        Socket client;
        String name;
        int turnOrder = connectioncounter;

        public int getStake() {
            return stake;
        }


        int stake = initialStakes;
        ArrayList<Card> hand = new ArrayList<>();
        private String listHand(){
            String s = "\nYour deck currently contains:\n";
            for (Card d: this.hand){
            s = s +(d.toString()) + "\n";
            }

            return s;
        }

        private int handScore(){
            int score=0;
            int acecount = 0;
            for (Card card : hand) {
                int cardScore = card.getScore();
                if (cardScore == 11){
                    acecount ++;
                }

                score = score + cardScore;
            }
            if (score > 21){
                score = score - (acecount * 10);
            }
            return score;
        }

        private void clientPrint(String printData) throws IOException{
            PrintWriter writer = new PrintWriter(client.getOutputStream(),true);
            writer.println(printData);

//            writer.println("New card: "+sentCard.toString());
        }



        public clientThread(Socket client) throws IOException{
            String line;
            this.client = client;
            System.out.println("Connection number: "+this.turnOrder);
            PrintWriter writer = new PrintWriter(client.getOutputStream(),true);
            int counter=0;


            while (true) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                line = reader.readLine();

                if (!line.equals("") && line.matches("[a-zA-Z]+")//alphabetical regex
                        && !playerList.containsKey(line) && !pendingList.containsKey(line)) {
                    writer.println("newName");
                    break;
                }
                else if(!line.matches("[a-zA-Z]+")){
                    writer.println("wrong format");
                }
                else {
                    writer.println("Name Taken.");
                }
            }


            this.name = line;
            pendingList.put(this.name, this);
            //clientList.put(this.name, this);
            System.out.println("Username: "+this.name);
            if (inProgress) {
                this.clientPrint("Please wait - there is currently a game in progress - shouldn't be long!");
            }

        }
    }


    public static void main(String[] args){

        new Server().runServer();
    }

    public void runServer(){
        System.out.println("------Danny's 21 Server------");
        System.out.println("How many tokens for each player?");
        Scanner getInfo = new Scanner(System.in);
        this.initialStakes = getInfo.nextInt();
        System.out.println("How many tokens per stake?");
        this.tokensPerDeal = getInfo.nextInt();
        System.out.println("How many initial players? (2-7)");
        this.numOfPlayers = getInfo.nextInt();
        System.out.println("------Waiting for players------");
        ServerSocket server = null;
        try{
            server = new ServerSocket(6969);
            new logicThread().start();
            while(true){
                Socket client = server.accept();
                connectioncounter++;
                System.out.println("New client connected - "+connectioncounter+" client(s) currently connected");
                new clientThread(client).start();
                System.out.println(pendingList);
                }

        }catch(IOException e){
            e.printStackTrace();
        }finally{
            if(server != null){
                try{
                    server.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
