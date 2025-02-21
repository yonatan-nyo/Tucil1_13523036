package tucilone.stime;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import tucilone.stime.utils.PuzzlePiece;

public class PrimaryController {
    @FXML
    private Button fileUploadButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private TextArea outputTextArea;

    @FXML
    private Canvas canvas;

    @FXML
    private Pane imagePane;

    private List<PuzzlePiece> puzzlePieces = new ArrayList<>();
    private int rows, columns, piecesCount;
    private char[][] board;
    private int iterationCount;
    private double lastExecutionTime;

    private boolean isValidInputPiece(char c) {
        return c == ' ' || (c >= 'A' && c <= 'Z');
    }

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
                    boolean allLinesValid = true;
                    List<String> lines = new ArrayList<>();
                    int maxCol = 0;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                        maxCol = Math.max(maxCol, line.length());
                        // check if all characters are valid
                        for (char c : line.toCharArray()) {
                            if (!isValidInputPiece(c)) {
                                allLinesValid = false;
                                break;
                            }
                        }
                    }

                    if (!allLinesValid) {
                        displayError("Invalid characters found in the puzzle pieces.");
                        return;
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
        Platform.runLater(() -> {
            // clear canvas
            clearGeneratedImage();
            // clear output text area
            outputTextArea.clear();
            outputTextArea.appendText("Error: " + message + "\n");
        });
    }

    private void handleSolve() {
        clearGeneratedImage();
        board = new char[rows][columns];
        for (char[] row : board) {
            Arrays.fill(row, '.');
        }

        iterationCount = 0;
        System.out.println("Starting puzzle solving...");

        long startTime = System.nanoTime();
        boolean placedAll = solvePuzzle(0, puzzlePieces);
        long endTime = System.nanoTime();
        lastExecutionTime = (endTime - startTime) / 1_000_000.0;
        boolean solved = false;

        if (placedAll) {
            // check if the whole board is filled (not '.' character)
            solved = true;
            for (char[] row : board) {
                for (char cell : row) {
                    if (cell == '.') {
                        solved = false;
                        break;
                    }
                }
            }
        }

        outputTextArea.clear();
        if (solved) {
            outputTextArea.appendText("Solution found:\n");
            System.out.println("Solution found!");

            drawGeneratedImage();

            for (char[] row : board) {
                outputTextArea.appendText(new String(row) + "\n");
            }

            outputTextArea.appendText("Execution time: " + lastExecutionTime + " ms\n");
            outputTextArea.appendText("Iteration count: " + iterationCount + "\n");

        } else {
            outputTextArea.appendText("No solution found.\n");
            System.out.println("No solution found.");
            outputTextArea.appendText("Execution time: " + lastExecutionTime + " ms\n");
            outputTextArea.appendText("Iteration count: " + iterationCount + "\n");
        }
    }

    private void clearGeneratedImage() {
        Platform.runLater(() -> {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()); // Clears the entire canvas
        });
    }

    private void drawGeneratedImage() {
        if (canvas == null)
            return;

        if (board == null || board.length == 0 || board[0].length == 0) {
            System.out.println("Board is not initialized or empty!");
            return;
        }

        int cellSize = 20;
        double maxWidth = imagePane.getWidth(); // Get Pane's width
        double maxHeight = imagePane.getHeight(); // Get Pane's height

        double calculatedWidth = Math.min(columns * cellSize, maxWidth);
        double calculatedHeight = Math.min(rows * cellSize, maxHeight);

        // Ensure canvas fits within container
        canvas.setWidth(calculatedWidth);
        canvas.setHeight(calculatedHeight);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Define 30 colors mapped to characters
        Map<Character, String> colorMap = new HashMap<>();
        char[] uniqueChars = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
                'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '.' };
        String[] hexColors = {
                "#FF5733", "#33FF57", "#3357FF", "#FF33A1", "#A133FF", "#33FFA1",
                "#FFD700", "#7CFC00", "#DC143C", "#8A2BE2", "#FF4500", "#00CED1",
                "#FF1493", "#ADFF2F", "#20B2AA", "#9932CC", "#FF6347", "#4682B4",
                "#EE82EE", "#3CB371", "#8B0000", "#556B2F", "#FF8C00", "#4169E1",
                "#9ACD32", "#D2691E", "#000000"
        };

        for (int i = 0; i < uniqueChars.length; i++) {
            colorMap.put(uniqueChars[i], hexColors[i]);
        }

        // Draw grid with colors based on board characters
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                char cellChar = board[i][j];
                String color = colorMap.getOrDefault(cellChar, "#FFFFFF");

                gc.setFill(Color.web(color));
                gc.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
            }
        }

        // Context Menu for right-click
        ContextMenu contextMenu = new ContextMenu();
        MenuItem saveImageItem = new MenuItem("Save Image");

        saveImageItem.setOnAction(event -> saveCanvasAsImage());

        contextMenu.getItems().add(saveImageItem);

        // Add right-click event
        canvas.setOnContextMenuRequested(event -> {
            contextMenu.show(canvas, event.getScreenX(), event.getScreenY());
        });
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void saveCanvasAsImage() {
        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, writableImage);

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fileChooser.setTitle("Save Image");
        fileChooser.setInitialFileName("generated_image.png");

        File file = fileChooser.showSaveDialog(imagePane.getScene().getWindow());

        if (file != null) {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);

            // Show loading indicator
            loadingIndicator.setVisible(true);

            new Thread(() -> {
                try {
                    // Save the image in a separate thread
                    javax.imageio.ImageIO.write(bufferedImage, "png", file);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // Hide loading indicator after saving is complete
                    Platform.runLater(() -> loadingIndicator.setVisible(false));
                }
            }).start(); // Start background thread
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

    @FXML
    public void downloadSolution() {
        StringBuilder solutionText = new StringBuilder();

        solutionText.append("Solution:\n");
        for (char[] row : board) {
            solutionText.append(new String(row)).append("\n");
        }
        solutionText.append("\nExecution time: ").append(lastExecutionTime).append(" ms\n");
        solutionText.append("Iteration count: ").append(iterationCount).append("\n");

        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Solution");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));

        // Show save dialog
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fileWriter = new FileWriter(fileChooser.getSelectedFile() + ".txt")) {
                fileWriter.write(solutionText.toString());
            } catch (IOException e) {
                displayError("Error: Unable to save the file.");
            }
        }
    }
}
