package tucilone.stime.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PuzzlePiece {
    private int currentCols = 0;
    private int currentRows = 0;
    public char[][] piece;
    private List<PuzzlePiece> uniqueVariations = null; // Cache for variations

    public int getRows() {
        return piece.length;
    }

    public int getCols() {
        return piece[0].length;
    }

    public PuzzlePiece(char[][] piece) {
        this.piece = piece;
    }

    public char[][] getPiece() {
        return piece;
    }

    public boolean addLine(char[] line) {
        if (currentCols == piece.length - 1 || line.length > piece[0].length) {
            return false;
        }
        System.arraycopy(line, 0, piece[currentCols], 0, line.length);
        currentCols++;
        currentRows = Math.max(currentRows, line.length);
        return true;
    }

    public PuzzlePiece rotate90() {
        int rows = piece.length;
        int cols = piece[0].length;
        char[][] rotated = new char[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotated[j][rows - 1 - i] = piece[i][j];
            }
        }
        return new PuzzlePiece(rotated);
    }

    public PuzzlePiece rotate180() {
        return rotate90().rotate90();
    }

    public PuzzlePiece rotate270() {
        return rotate90().rotate90().rotate90();
    }

    public PuzzlePiece flipHorizontal() {
        int rows = piece.length;
        int cols = piece[0].length;
        char[][] flipped = new char[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flipped[i][cols - 1 - j] = piece[i][j];
            }
        }
        return new PuzzlePiece(flipped);
    }

    public PuzzlePiece flipVertical() {
        int rows = piece.length;
        int cols = piece[0].length;
        char[][] flipped = new char[rows][cols];
        for (int i = 0; i < rows; i++) {
            flipped[rows - 1 - i] = Arrays.copyOf(piece[i], cols);
        }
        return new PuzzlePiece(flipped);
    }

    public void printPiece() {
        for (char[] row : piece) {
            for (char c : row) {
                System.out.print(c);
            }
            System.out.println();
        }
    }

    // private String getStringRepresentation() {
    // StringBuilder sb = new StringBuilder();
    // for (char[] row : piece) {
    // sb.append(new String(row)).append(";");
    // }
    // return sb.toString();
    // }

    public List<PuzzlePiece> generateUniqueVariations() {
        if (uniqueVariations != null) {
            return uniqueVariations; // Return cached variations
        }

        PuzzlePiece[] transformations = {
                this, rotate90(), rotate180(), rotate270(),
                flipHorizontal(), flipHorizontal().rotate90(), flipHorizontal().rotate180(),
                flipHorizontal().rotate270()
        };

        List<PuzzlePiece> variations = new ArrayList<>();
        variations.addAll(Arrays.asList(transformations));

        // Set<String> seen = new HashSet<>();
        // for (PuzzlePiece p : transformations) {
        // String key = p.getStringRepresentation();
        // if (!seen.contains(key)) {
        // seen.add(key);
        // variations.add(p);
        // }
        // }
        uniqueVariations = variations; // Cache result
        return variations;
    }

    public List<PuzzlePiece> generateAllVariations() {
        return generateUniqueVariations();
    }

}
