package tucilone.stime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import tucilone.stime.utils.PuzzlePiece;

public class PrimaryController {
    @FXML
    private Button fileUploadButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private TextArea outputTextArea;

    private List<PuzzlePiece> puzzlePieces = new ArrayList<>();
    private int rows, columns, piecesCount;
    private char[][] board;
    private int iterationCount;
    private double lastExecutionTime;

    @SuppressWarnings("unused")
    @FXML
    private void handleFileUpload() {
        loadingIndicator.setVisible(true);

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showOpenDialog(fileUploadButton.getScene().getWindow());

        if (file != null) {
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;

                    // Read rows, columns, count
                    if ((line = reader.readLine()) != null) {
                        String[] values = line.trim().split(" ");
                        if (values.length != 3) {
                            displayError(
                                    "The first line must contain exactly 3 numbers (rows, columns, and pieces count).");
                            return;
                        }

                        rows = Integer.parseInt(values[0]);
                        columns = Integer.parseInt(values[1]);
                        piecesCount = Integer.parseInt(values[2]);

                        if (piecesCount > 26) {
                            displayError("The number of pieces must be ≤ 26.");
                            return;
                        }
                    } else {
                        displayError("Missing dimensions in the first line.");
                        return;
                    }

                    // Read mode
                    String mode;
                    if ((line = reader.readLine()) != null) {
                        mode = line.trim();
                    } else {
                        displayError("Missing mode in the second line.");
                        return;
                    }

                    // Read all lines
                    List<String> lines = new ArrayList<>();
                    int maxCol = 0;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                        maxCol = Math.max(maxCol, line.length());
                    }

                    // Extract pieces
                    puzzlePieces = getAllPiecesFromLines(lines, lines.size(), maxCol);

                    // if puzzle piece count is not equal to piecesCount throw error
                    if (puzzlePieces.size() != piecesCount) {
                        displayError(String.format("Incorrect amount of pieces input %d received %d",
                                puzzlePieces.size(), piecesCount));
                    }

                    // Debug: Print pieces
                    System.out.println("\nLoaded Puzzle Pieces:");
                    for (PuzzlePiece piece : puzzlePieces) {
                        piece.printPiece();
                        System.out.println();
                    }

                    handleSolve();

                } catch (IOException e) {
                    displayError("Error reading file: " + e.getMessage());
                } catch (NumberFormatException e) {
                    displayError("Invalid number format in file.");
                } finally {
                    // Hide the loading UI thread
                    Platform.runLater(() -> loadingIndicator.setVisible(false));
                }
            }).start();
        } else {
            // Hide if no file selected
            loadingIndicator.setVisible(false);
        }
    }

    private List<PuzzlePiece> getAllPiecesFromLines(List<String> lines, int rows, int columns) {
        List<PuzzlePiece> pieces = new ArrayList<>();
        boolean[][] visited = new boolean[rows][columns];
        char[][] grid = new char[rows][columns];

        for (int i = 0; i < rows; i++) {
            char[] rowChars = lines.get(i).toCharArray();
            Arrays.fill(grid[i], '.');
            System.arraycopy(rowChars, 0, grid[i], 0, rowChars.length);
        }

        // 4-way movement
        int[] dRow = { -1, 1, 0, 0 };
        int[] dCol = { 0, 0, -1, 1 };

        // DFS to extract a piece
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (grid[i][j] != '.' && !visited[i][j]) { // new piece
                    List<int[]> cells = new ArrayList<>();
                    char pieceChar = grid[i][j];

                    // DFS gather all connected cells of piece
                    dfs(grid, visited, i, j, pieceChar, cells, dRow, dCol);
                    PuzzlePiece piece = extractPiece(cells, grid);
                    pieces.add(piece);
                }
            }
        }

        return pieces;
    }

    private void dfs(char[][] grid,
            boolean[][] visited,
            int r, int c,
            char pieceChar,
            List<int[]> cells,
            int[] dRow, int[] dCol) {
        int rowCount = grid.length;

        // Ensure column count is valid for this row
        if (r < 0 || r >= rowCount || c < 0 || c >= grid[r].length || visited[r][c] || grid[r][c] != pieceChar) {
            return;
        }

        visited[r][c] = true;
        cells.add(new int[] { r, c });

        // Explore all 4 directions
        for (int d = 0; d < 4; d++) {
            int newRow = r + dRow[d];
            int newCol = c + dCol[d];

            // Ensure new position is within bounds before calling DFS
            if (newRow >= 0 && newRow < rowCount && newCol >= 0 && newCol < grid[newRow].length) {
                dfs(grid, visited, newRow, newCol, pieceChar, cells, dRow, dCol);
            }
        }
    }

    private PuzzlePiece extractPiece(List<int[]> cells, char[][] grid) {
        // Find bounding box
        int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;

        for (int[] cell : cells) {
            minRow = Math.min(minRow, cell[0]);
            maxRow = Math.max(maxRow, cell[0]);
            minCol = Math.min(minCol, cell[1]);
            maxCol = Math.max(maxCol, cell[1]);
        }

        int pieceRows = maxRow - minRow + 1;
        int pieceCols = maxCol - minCol + 1;
        char[][] pieceMatrix = new char[pieceRows][pieceCols];

        // Fill with empty space ('.')
        for (int i = 0; i < pieceRows; i++) {
            Arrays.fill(pieceMatrix[i], '.');
        }

        // Copy the extracted piece into the new matrix
        for (int[] cell : cells) {
            int r = cell[0] - minRow;
            int c = cell[1] - minCol;
            pieceMatrix[r][c] = grid[cell[0]][cell[1]];
        }

        return new PuzzlePiece(pieceMatrix);
    }

    private void displayError(String message) {
        System.err.println("Error: " + message);
    }

    private void handleSolve() {
        board = new char[rows][columns];
        for (char[] row : board) {
            Arrays.fill(row, '.');
        }

        iterationCount = 0;
        System.out.println("Starting puzzle solving...");

        long startTime = System.nanoTime();
        boolean solved = solvePuzzle(0, puzzlePieces);
        long endTime = System.nanoTime();
        lastExecutionTime = (endTime - startTime) / 1_000_000.0;

        outputTextArea.clear();
        if (solved) {
            outputTextArea.appendText("Solution found:\n");
            System.out.println("Solution found!");
            for (char[] row : board) {
                outputTextArea.appendText(new String(row) + "\n");
            }
            outputTextArea.appendText("Execution time: " + lastExecutionTime + " ms\n");
            outputTextArea.appendText("Iteration count: " + iterationCount + "\n");

        } else {
            outputTextArea.appendText("No solution found. Ensure pieces fit within the grid.\n");
            System.out.println("No solution found.");
            outputTextArea.appendText("Execution time: " + lastExecutionTime + " ms\n");
            outputTextArea.appendText("Iteration count: " + iterationCount + "\n");
        }
    }

    private boolean solvePuzzle(int pieceIndex, List<PuzzlePiece> pieces) {
        if (pieceIndex == pieces.size()) {
            return true;
        }

        PuzzlePiece piece = pieces.get(pieceIndex);
        List<PuzzlePiece> variations = piece.generateAllVariations();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                for (PuzzlePiece variation : variations) {
                    iterationCount++;
                    if (canPlacePiece(r, c, variation, board)) {
                        placePiece(r, c, variation, board);

                        // debug print the board
                        // System.out.println(
                        // "Piece: " + pieceIndex + ", Row: " + r + ", Col: " + c);
                        // System.out.println("-----");
                        // variation.printPiece();
                        // System.out.println("-----");
                        // for (char[] row : board) {
                        // System.out.println(new String(row));
                        // }
                        // System.out.println();

                        if (solvePuzzle(pieceIndex + 1, pieces)) {
                            return true;
                        }

                        removePiece(r, c, variation, board);
                    }
                }

            }
        }

        // If no valid placement is found, return false to backtrack
        return false;
    }

    private boolean canPlacePiece(int r, int c, PuzzlePiece piece, char[][] board) {
        int pieceRows = piece.getRows();
        int pieceCols = piece.getCols();

        if (r + pieceRows > rows || c + pieceCols > columns) {
            return false;
        }

        for (int i = 0; i < pieceRows; i++) {
            for (int j = 0; j < pieceCols; j++) {
                if (piece.getPiece()[i][j] != '.' && board[r + i][c + j] != '.') {
                    return false;
                }
            }
        }

        return true;
    }

    private void placePiece(int r, int c, PuzzlePiece piece, char[][] board) {
        int pieceRows = piece.getRows();
        int pieceCols = piece.getCols();

        for (int i = 0; i < pieceRows; i++) {
            for (int j = 0; j < pieceCols; j++) {
                if (piece.getPiece()[i][j] != '.') {
                    board[r + i][c + j] = piece.getPiece()[i][j];
                }
            }
        }
    }

    private void removePiece(int r, int c, PuzzlePiece piece, char[][] board) {
        int pieceRows = piece.getRows();
        int pieceCols = piece.getCols();

        for (int i = 0; i < pieceRows; i++) {
            for (int j = 0; j < pieceCols; j++) {
                if (piece.getPiece()[i][j] != '.') {
                    board[r + i][c + j] = '.';
                }
            }
        }
    }
}
