import java.io.*;
import java.net.*;
import java.util.Arrays;

public class GomokuServer {
    private static final int BOARD_SIZE = 19;
    private static final char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
    private static final int PORT = 5000;
    private static Socket player1 = null, player2 = null;
    private static DataInputStream input1 = null, input2 = null;
    private static DataOutputStream output1 = null, output2 = null;
    private static boolean isPlayer1Turn = true;

    public static void main(String[] args) throws IOException {
        // 보드 초기화
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
        try {
            boolean forbiddenMoveOccurred = false;
            while (true) {
                DataInputStream currentInput = isPlayer1Turn ? input1 : input2;
                DataOutputStream currentOutput = isPlayer1Turn ? output1 : output2;
                DataOutputStream otherOutput = isPlayer1Turn ? output2 : output1;
                char currentSymbol = isPlayer1Turn ? 'X' : 'O';

                if (!forbiddenMoveOccurred) {
                    currentOutput.writeUTF("Your turn.");
                }

                String move;
                try {
                    move = currentInput.readUTF();
                } catch (IOException e) {
                    System.out.println("A player disconnected.");
                    resetPlayers();
                    break;
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

                // 삼삼/사사/장목 금지 규칙 검사
                if (isForbiddenMove(row, col, currentSymbol)) {
                    currentOutput.writeUTF("Forbidden move! Try again.");
                    board[row][col] = '.'; // Undo the move
                    forbiddenMoveOccurred = true; // 금지된 수 발생 플래그 설정
                    continue;
                }

                forbiddenMoveOccurred = false; // 금지된 수가 없으면 플래그 초기화

                broadcastBoard();

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
            }
        } catch (IOException e) {
            System.out.println("Error during game: " + e.getMessage());
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

    private static boolean isForbiddenMove(int row, int col, char symbol) {
//        if (symbol == 'O') return false; // 백돌은 금지 규칙 없음

        // 삼삼 금지: 열린 삼이 2개 이상 발생
        int threeCount = countOpenThree(row, col, symbol);
        if (threeCount >= 2) return true;

        // 사사 금지: 열린 사가 2개 이상 발생
        int fourCount = countOpenFour(row, col, symbol);
        if (fourCount >= 2) return true;

        // 장목 금지: 6개 이상의 돌이 연속된 경우
        if (isOverline(row, col, symbol)) return true;

        return false; // 금지되지 않은 수
    }

    private static boolean isOverline(int row, int col, char symbol) {
        // 장목(6개 이상 연속) 여부 검사
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
            if (count > 5) return true;
        }
        return false;
    }

    private static int countOpenThree(int row, int col, char symbol) {
        return countPatterns(row, col, symbol, 3, true);
    }

    private static int countOpenFour(int row, int col, char symbol) {
        return countPatterns(row, col, symbol, 4, true);
    }

    private static int countPatterns(int row, int col, char symbol, int length, boolean checkOpen) {
        int count = 0;
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

        for (int[] dir : directions) {
            int consecutive = 1; // 현재 연속된 돌 개수
            boolean openStart = false, openEnd = false;

            // ← 방향
            int nx = row - dir[0], ny = col - dir[1];
            while (isValid(nx, ny) && board[nx][ny] == symbol) {
                consecutive++;
                nx -= dir[0];
                ny -= dir[1];
            }
            if (isValid(nx, ny) && board[nx][ny] == '.') openStart = true;

            // → 방향
            nx = row + dir[0];
            ny = col + dir[1];
            while (isValid(nx, ny) && board[nx][ny] == symbol) {
                consecutive++;
                nx += dir[0];
                ny += dir[1];
            }
            if (isValid(nx, ny) && board[nx][ny] == '.') openEnd = true;

            // 패턴이 유효한지 검사
            if (consecutive == length && (openStart && openEnd)) {
                count++;
            }
        }
        return count;
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

    private static boolean isValid(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
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
        System.out.println("Players reset. Waiting for new connections...");
    }
}
