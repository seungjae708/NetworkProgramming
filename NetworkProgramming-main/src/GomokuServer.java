import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Stack;

public class GomokuServer {
    private static final int BOARD_SIZE = 19;
    private static final char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
    private static final int PORT = 5000;
    private static Socket player1 = null, player2 = null;
    private static DataInputStream input1 = null, input2 = null;
    private static DataOutputStream output1 = null, output2 = null;
    private static boolean isPlayer1Turn = true;
    private static final Stack<int[]> moveHistory = new Stack<>(); // 수 기록 저장

    public static void main(String[] args) throws IOException {
        for (char[] row : board) Arrays.fill(row, '.');

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running... Waiting for players...");

        while (true) {
            if (player1 == null) {
                System.out.println("Waiting for Player 1...");
                player1 = serverSocket.accept();
                System.out.println("Player 1 connected.");
                input1 = new DataInputStream(player1.getInputStream());
                output1 = new DataOutputStream(player1.getOutputStream());
                output1.writeUTF("You are Player 1 (X).");
            }

            if (player2 == null) {
                System.out.println("Waiting for Player 2...");
                player2 = serverSocket.accept();
                System.out.println("Player 2 connected.");
                input2 = new DataInputStream(player2.getInputStream());
                output2 = new DataOutputStream(player2.getOutputStream());
                output2.writeUTF("You are Player 2 (O).");
            }

            if (player1 != null && player2 != null) {
                playGame();
            }
        }
    }

    private static void playGame() throws IOException {
        while (true) {
            DataInputStream currentInput = isPlayer1Turn ? input1 : input2;
            DataOutputStream currentOutput = isPlayer1Turn ? output1 : output2;
            DataOutputStream otherOutput = isPlayer1Turn ? output2 : output1;
            char currentSymbol = isPlayer1Turn ? 'X' : 'O';

            currentOutput.writeUTF("Your turn.");

            String move;
            try {
                move = currentInput.readUTF();
            } catch (IOException e) {
                System.out.println("A player disconnected.");
                resetPlayers();
                break;
            }

            if (move.startsWith("CHAT:")) {
                otherOutput.writeUTF(move);
                continue;
            }

            if (move.equals("UNDO_REQUEST")) {
                otherOutput.writeUTF("UNDO_REQUEST_FROM_OTHER_PLAYER");
                continue;
            }

            if (move.equals("UNDO_ACCEPT")) {
                boardResetLastMove();
                broadcastBoard();
                continue;
            }

            String[] parts = move.split(",");
            int row, col;
            try {
                row = Integer.parseInt(parts[0]);
                col = Integer.parseInt(parts[1]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                currentOutput.writeUTF("Invalid move. Try again.");
                continue;
            }

            if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE || board[row][col] != '.') {
                currentOutput.writeUTF("Invalid move. Try again.");
                continue;
            }

            board[row][col] = currentSymbol;
            moveHistory.push(new int[]{row, col}); // 최근 수 기록

            if (checkWin(row, col, currentSymbol)) {
                currentOutput.writeUTF("You win!");
                otherOutput.writeUTF("You lose!");
                resetPlayers();
                break;
            }

            if (isBoardFull()) {
                currentOutput.writeUTF("Draw!");
                otherOutput.writeUTF("Draw!");
                resetPlayers();
                break;
            }

            isPlayer1Turn = !isPlayer1Turn;
            broadcastBoard();
        }
    }

    private static void boardResetLastMove() {
        if (!moveHistory.isEmpty()) {
            int[] lastMove = moveHistory.pop();
            board[lastMove[0]][lastMove[1]] = '.';
            isPlayer1Turn = !isPlayer1Turn;
            System.out.println("Last move undone.");
        } else {
            System.out.println("No moves to undo.");
        }
    }

    private static void broadcastBoard() throws IOException {
        StringBuilder boardState = new StringBuilder("Current board:\n");
        for (char[] row : board) {
            boardState.append(String.valueOf(row)).append("\n");
        }
        if (output1 != null) output1.writeUTF(boardState.toString());
        if (output2 != null) output2.writeUTF(boardState.toString());
    }

    private static boolean checkWin(int row, int col, char symbol) {
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1;
            for (int d = -1; d <= 1; d += 2) {
                int r = row, c = col;
                while (true) {
                    r += dir[0] * d;
                    c += dir[1] * d;
                    if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == symbol) {
                        count++;
                    } else {
                        break;
                    }
                }
            }
            if (count >= 5) return true;
        }
        return false;
    }

    private static boolean isBoardFull() {
        for (char[] row : board) {
            for (char cell : row) {
                if (cell == '.') return false;
            }
        }
        return true;
    }

    private static void resetPlayers() {
        try {
            if (player1 != null) player1.close();
            if (player2 != null) player2.close();
        } catch (IOException e) {
            System.out.println("Error closing connections: " + e.getMessage());
        }
        player1 = null;
        player2 = null;
        input1 = null;
        input2 = null;
        output1 = null;
        output2 = null;
        isPlayer1Turn = true;
        for (char[] row : board) Arrays.fill(row, '.');
        moveHistory.clear(); // 기록 초기화
        System.out.println("Players reset. Waiting for new connections...");
    }
}
