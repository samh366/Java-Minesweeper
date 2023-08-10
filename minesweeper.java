import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Cell {
    boolean covered = true;
    int num = 0;
    boolean mine = false;
    boolean flagged = false;

    public String toString() {
        if (flagged) {
            return "F";
        } else if (covered) {
            return "-";
        } else if (mine) {
            return "M";
        } else if (num != 0) {
            return Integer.toString(num);
        } else {
            return " ";
        }
    }

    // Getters
    public boolean isMine() {return mine;}
    public boolean isFlagged() {return flagged;}
    public int getNum() {return num;}
    public boolean isCovered() {return covered;}
    public boolean isBlank() {
        if (!flagged && !covered && !mine && num == 0) {
            return true;
        }
        return false;
    }

    // Setters
    public void setMine() {mine = true;}
    public void setNum(int inp) {num = inp;}
    public void uncover() {covered = false;}
    public boolean setFlag(boolean val) {
        // Returns true if the value of flag was changed
        if (flagged != val) {
            flagged = val;
            return true;
        }
        return false;
    }
}

class Board {
    final int STARTMINES = 10;
    int numFlags = 10;
    boolean running = true;
    boolean addedBombs = false;
    ArrayList<ArrayList<Cell>> cells = generateEmptyBoard();
    int status = 0;
    Random rand = new Random();


    public ArrayList<ArrayList<Cell>> generateEmptyBoard() {
        ArrayList<ArrayList<Cell>> cells = new ArrayList<ArrayList<Cell>>();

        for (int y = 0; y < 9; y++) {
            cells.add(new ArrayList<>());
            for (int x = 0; x < 9; x++) {
                cells.get(y).add(new Cell());
            }
        }
        return cells;
    }

    public ArrayList<ArrayList<Cell>> getBoard() {
        return cells;
    }

    public String toString() {
        String section2 = "";
        // Loop through the 9 rows
        for (int y=0;y<9;y++) {
            String line = Integer.toString(y) + "|";
            // For c in cells[y]
            for (Cell c: cells.get(y)) {
                line += c.toString();
            }
            line += "|"+"\n";
            section2 += line;
        }

        String section1 = " +---------+\n";
        section1 += String.format(" |  M:%02d   |\n", numFlags);
        section1 += " +---------+\n" + " |ABCDEFGHI|\n" + " +---------+\n";

        return section1 + section2 + " +---------+\n";

    }

    public void setup(int[] start) {
        int bombsLeft = STARTMINES;
        while (bombsLeft != 0) {
            int[] coord = {rand.nextInt(9), rand.nextInt(9)};

            if (!(Arrays.equals(coord, start)) && !cells.get(coord[1]).get(coord[0]).isMine()) {
                cells.get(coord[1]).get(coord[0]).setMine();
                // Update cells around the bomb
                addNumbers(coord);
                bombsLeft -= 1;

            }
        }
    }

    public void addNumbers(int[] bomb) {
        // Updates the numbers around a bomb
        int[] nums = {-1, 0, 1};
        for (int y: nums) {
            y += bomb[1];
            for (int x: nums) {
                x += bomb[0];
                // Check coords are in range
                if (x < 9 && x > -1 && y < 9 && y > -1 && !(bomb[0] == x && bomb[1] == y)) {
                    // Make sure the cell we are updating is not a mine
                    if (!cells.get(y).get(x).isMine()) {
                        cells.get(y).get(x).setNum(cells.get(y).get(x).getNum()+1);
                    }
                }
            }
        }
    }

    public void clearEmpty(int[] start, ArrayList<Cell> visited) {
        // Clears the empty cells around a starting cell
        visited.add(cells.get(start[1]).get(start[0]));
        int[] nums = {-1, 0, 1};
        for (int y: nums) {
            y += start[1];
            for (int x: nums) {
                x += start[0];
                if (x < 9 && x > -1 && y < 9 && y > -1 && !(start[0] == x && start[1] == y)) {
                    cells.get(y).get(x).uncover();
                    if (cells.get(y).get(x).isBlank() && !visited.contains(cells.get(y).get(x))) {
                        int[] newStart = {x, y};
                        clearEmpty(newStart, visited);
                    }
                }
            }
        }
    }

    public void action(String input) {
        // Equiv to getInput() in python
        // Check the input against a regular expression
        Pattern pattern = Pattern.compile("[F|U|C]\\[[A-I][0-8]\\]", Pattern.CASE_INSENSITIVE); // Reg expression/pattern
        Matcher matcher = pattern.matcher(input); // Checks that a given string matches an expression

        if (matcher.find() == true) {
            int x = charToNum(input.charAt(2));
            int y = Integer.parseInt(""+input.charAt(3));
            char action = input.charAt(0);
            Cell cell = cells.get(y).get(x);
            // Different options
            switch(action) {
                case 'F':
                    // Add a flag
                    if (cell.isCovered()) {
                        if (cell.setFlag(true)) {
                            numFlags -= 1;
                        } else {
                            System.out.println("This cell is already flagged!");
                        }
                    } else {
                        System.out.println("Cannot flag an uncovered cell!");
                    }
                    break;
                
                case 'U':
                    // Undo a flag
                    if (cell.setFlag(false)) {
                        numFlags += 1;
                    } else {
                        System.out.println("There isn't a flag on this cell!");
                    }
                    break;
                
                case 'C':
                    // Uncover a cell, if not flagged 
                    if (!cell.isFlagged() && cell.isCovered()) {
                        cell.uncover();
                        int[] coords = {x, y};
                        // Do a bunch of game checks each time
                        if (addedBombs == false) {
                            setup(coords);
                            addedBombs = true;
                        }
                        if (cell.isBlank()) {
                            ArrayList<Cell> visited = new ArrayList<Cell>();
                            clearEmpty(coords, visited);
                        }
                        if (cell.isMine()) {
                            lose();
                        }
                    } else {
                        System.out.println("Cannot uncover this cell");
                    }
                    break;
            }
        } else {
            System.out.println("Invalid command, example command: F[A0]");
        }
    }

    public void checkWin() {
        if (running) {
            // Win if numFlags is 0 and all the flags are correctly placed
            if (numFlags == 0) {
                boolean check = true;
                for (ArrayList<Cell> row: cells) {
                    for (Cell cell: row) {
                        // Check for each cell
                        // If flagged and not a mine, or not flagged and a mine, there is not a win
                        if ((cell.isMine() && !cell.isFlagged()) || (cell.isFlagged() && !cell.isMine())) {
                            check = false;
                        }
                    }
                }
                // If passes the checks, win the game
                if (check == true) {
                    status = 1;
                    running = false;
                }
            }
        }
    }

    public int charToNum(char letter) {
        // Converts [A-I] to [0-8]
        return ((int) letter)-65;
    }

    public boolean isRunning() {
        return running;
    }

    public void win() {
        running = false;
        status = 1;
    }

    public void lose() {
        running = false;
        status = 2;
    }

    public int getStatus() {
        // Returns the staus of the game
        // 0 - Running
        // 1 - Win
        // 2 - Lose
        checkWin();
        return status;
    }

}

public class minesweeper {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String playAgain = "y";
        while (playAgain.equals("y")) {
            Board game = new Board();
            while (game.isRunning()) {
                System.out.println(game.toString());
                System.out.print("Enter Command:\nF - Flag\nU - Unflag\nC - Uncover\n>>> ");
                String input = scan.next();
                game.action(input);
                int status = game.getStatus();
                switch (status) {
                    case 1:
                        // Win
                        System.out.println(game.toString());
                        System.out.println("You Win! Congratulations!");
                        System.out.print("<Play again? (y/n)>\n>>> ");
                        playAgain = scan.next().toLowerCase();
                        break;
                    
                    case 2:
                        // Lose
                        System.out.println(game.toString());
                        System.out.println("You Lose! You clicked on a mine!");
                        System.out.print("<Play again? (y/n)>\n>>> ");
                        playAgain = scan.next().toLowerCase();
                        break;
                }
            }
        }
        scan.close();
    }
}


