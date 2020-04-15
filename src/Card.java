import java.util.Hashtable;
public class Card {
    private int value;
    private int suit;
    //potential values
    String[] suits = {"Clubs", "Hearts", "Spades", "Diamonds"};
    String[] values = {null, "Ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King"};

    //scores - aces always = 11. Our handScore() function deals with the 1/11 dichotomy at the scoring stage.
    // pseudocode: once handscore > 21, take away 10 points for each ace.
    Hashtable<String, Integer> scoreTable;

    {
        scoreTable = new Hashtable<>();
        scoreTable.put("Ace", 11);
        scoreTable.put("2", 2);
        scoreTable.put("3", 3);
        scoreTable.put("4", 4);
        scoreTable.put("5", 5);
        scoreTable.put("6", 6);
        scoreTable.put("7", 7);
        scoreTable.put("8", 8);
        scoreTable.put("9", 9);
        scoreTable.put("10", 10);
        scoreTable.put("Jack", 10);
        scoreTable.put("Queen", 10);
        scoreTable.put("King", 10);
    }

    public int getScore(){
        return scoreTable.get(values[this.value]);
        }

    public Card(int value, int suit){
        this.value = value;
        this.suit = suit;
    }
    public String toString() {
        return values[this.value] + " of " + suits[this.suit];
    }

}
