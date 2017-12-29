package qirkat;

import org.junit.Test;
import static org.junit.Assert.*;

public class MoreBoardTests {
    private final char[][] boardRepr = new char[][]{
        {'b', 'b', 'b', 'b', 'b'},
        {'b', 'b', 'b', 'b', 'b'},
        {'b', 'b', '-', 'w', 'w'},
        {'w', 'w', 'w', 'w', 'w'},
        {'w', 'w', 'w', 'w', 'w'}
    };

    private static final String INIT_BOARD =
            "  b b b b b\n  b b b b b\n  b b - w w\n  w w w w w\n  w w w w w";

    private final PieceColor currMove = PieceColor.WHITE;

    private static final String[] GAME1 =
    { "d3-c3", "b3-d3", "e3-c3", "a3-b3", "c3-a3"
    };

    private static final String GAME1_BOARD =
            " w w w w w\n  w w w w w\n  w - - - -\n  b b b b b\n  b b b b b";

    private static final String[] GAME2A =
    { "b2-b3", "b4-b2", "b1-b3", "b5-b4"
    };

    private static final String GAME2_BOARDA =
            " w w - w w\n  w w - w w\n  b b w w w\n  b b - b b\n  b b b b b";

    private static final String[] GAME2_B =
    {"a3-c1"
    };

    private static final String GAME2_BOARD_B =
            " w w b w w\n  w - - w w\n  - b w w w\n  b b - b b\n  b b b b b";


    /**
     * @return the String representation of the initial state. This will
     * be a string in which we concatenate the values from the bottom of
     * board upwards, so we can pass it into setPieces. Read the comments
     * in Board#setPieces for more information.
     *
     * For our current boardRepr, the String returned by
     * getInitialRepresentation is
     * "  w w w w w\n  w w w w w\n  b b - w w\n  b b b b b\n  b b b b b"
     *
     * We use a StringBuilder to avoid recreating Strings (because Strings
     * are immutable).
     */
    private String getInitialRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int i = boardRepr.length - 1; i >= 0; i--) {
            for (int j = 0; j < boardRepr[0].length; j++) {
                sb.append(boardRepr[i][j] + " ");
            }
            sb.deleteCharAt(sb.length() - 1);
            if (i != 0) {
                sb.append("\n  ");
            }
        }
        return sb.toString();
    }

    private Board getBoard() {
        Board b = new Board();
        b.setPieces(getInitialRepresentation(), currMove);
        return b;
    }

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(Move.parseMove(s));
        }
    }

    private void resetToInitialState(Board b) {
        b.setPieces(getInitialRepresentation(), currMove);
    }

    @Test
    public void testSomething1() {
        Board b = getBoard();
        makeMoves(b, GAME1);
        assertEquals(GAME1_BOARD, b.toString());
        resetToInitialState(b);
        assertEquals(INIT_BOARD, b);
    }

    @Test
    public void testSomething2() {
        Board b = getBoard();
        makeMoves(b, GAME2A);
        assertEquals(GAME2_BOARDA, b.toString());
        makeMoves(b, GAME2_B);
        assertEquals(GAME2_BOARD_B, b.toString());
    }
}
