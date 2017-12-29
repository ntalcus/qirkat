package qirkat;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author
 */
public class BoardTest {

    private static final String INIT_BOARD =
        "  b b b b b\n  b b b b b\n  b b - w w\n  w w w w w\n  w w w w w";

    private static final PieceColor WHITE = PieceColor.WHITE;
    private static final PieceColor BLACK = PieceColor.BLACK;
    private static final PieceColor EMPTY = PieceColor.EMPTY;

    private static final String[] GAME1 =
    { "c2-c3", "c4-c2",
      "c1-c3", "a3-c1",
      "c3-a3", "c5-c4",
      "a3-c5-c3",
    };

    private static final String GAME1_BOARD =
        "  b b - b b\n  b - - b b\n  - - w w w\n  w - - w w\n  w w b w w";

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(Move.parseMove(s));
        }
    }

    @Test
    public void testInit1() {
        Board b0 = new Board();
        assertEquals(INIT_BOARD, b0.toString());
    }

    @Test
    public void testMoves1() {
        Board b0 = new Board();
        makeMoves(b0, GAME1);
        assertEquals(GAME1_BOARD, b0.toString());
    }

    @Test
    public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        Board b2 = new Board(b0);
        PieceColor turn;
        for (int i = 0; i < GAME1.length; i += 1) {
            turn = b0.whoseMove();
            b0.undo();
        }
        boolean test = b1.equals(b0);
        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

    @Test
    public void testBoard() {
        Board b = new Board();
        String init = "-----"
                    + "-----"
                    + "--w--"
                    + "-----"
                    + "--b--";
        b.setPieces(init, WHITE);
    }

    @Test
    public void testGetMoves() {
        Board b = new Board();
        ArrayList<Move> moves = new ArrayList<Move>();
        String init = "-----"
                + "-----"
                + "--w--"
                + "-----"
                + "--b--";
        b.setPieces(init, WHITE);
        b.getMoves(moves);
        assertEquals(5, moves.size());
        String init2 = "wwwww ----- ----- ----- bbbbb";
        b.setPieces(init2, WHITE);
        moves.clear();
        b.getMoves(moves);
        assertEquals(9, moves.size());
        moves.clear();
        String init3 = "--b-- ----- ----- ----- --w--";
        b.setPieces(init3, WHITE);
        b.getMoves(moves);
        assertEquals(true, moves.size() == 0);
    }


    @Test
    public void testLegalMove() {
        Board b = new Board();
        ArrayList<Move> moves = new ArrayList<Move>();
        String init2 = "wwwww ----- ----- ----- bbbbb";
        b.setPieces(init2, WHITE);
        assertEquals(false, b.legalMove(Move.move('c', '3', 'd', '3')));
        b.getMoves(moves);
        for (Move move : moves) {
            assertEquals(true, b.legalMove(move));
        }
    }

    @Test
    public void testGetJumps() {
        Board b = new Board();
        ArrayList<Move> moves = new ArrayList<Move>();
        String init2 = "----- --b-- -bwb- --b-- -----";
        b.setPieces(init2, WHITE);
        b.getMoves(moves);
        assertEquals(4, moves.size());
        moves.clear();
        String init3 = "-b-b- --b-- --w-- ----- -----";
        b.setPieces(init3, WHITE);
        b.getMoves(moves);
        assertEquals(2, moves.size());
        moves.clear();
        String init4 = "-b-b- b-b-b --w-- --b-- -----";
        b.setPieces(init4, WHITE);
        b.getMoves(moves);
        assertEquals(3, moves.size());
        moves.clear();
        String init5 = "--b-- ----- ----- --b-- --w--";
        b.setPieces(init5, WHITE);
        b.getMoves(moves);
        assertEquals(1, moves.size());

    }


    @Test
    public void testCheckJumps() {
        Board b = new Board();
        ArrayList<Move> moves = new ArrayList<Move>();
        String init4 = "-b-b- b-b-b --w-- --b-- -----";
        b.setPieces(init4, WHITE);
        b.getMoves(moves);
        for (Move move : moves) {
            assertEquals(true, b.checkJump(move, false));
        }
        Move partial = Move.move('c', '3', 'c', '1');
        assertEquals(true, b.checkJump(partial, true));
        Move partial2 = Move.move('c', '1', 'a', '1');
        partial = Move.move('c', '3', 'c', '1', partial2);
        assertEquals(true, b.checkJump(partial, true));
    }


    @Test
    public void testInvalidMoveLeftRight() {
        Board b = new Board();
        ArrayList<Move> moves = new ArrayList<Move>();
        String init4 = "--w-- ----- ----- ----- --b--";
        b.setPieces(init4, WHITE);
        b.makeMove('c', '1', 'b', '1');
        b.makeMove('c', '5', 'd', '5');
        b.getMoves(moves);
        assertEquals(2, moves.size());
        b.makeMove('b', '1', 'b', '2');
        b.makeMove('d', '5', 'd', '4');
        moves.clear();
        b.getMoves(moves);
        assertEquals(5, moves.size());
    }

    @Test
    public void testMovingIntoPreviouslyOccupiedSpot() {
        Board b = new Board();
        ArrayList<Move> moves = new ArrayList<Move>();
        String init4 = "w---- w---- ----- ----b ----b";
        b.setPieces(init4, WHITE);
        System.out.println(b.toString());
        b.makeMove('a', '2', 'b', '2');
        b.makeMove('e', '4', 'd', '4');
        b.makeMove('b', '2', 'c', '2');
        b.makeMove('d', '4', 'd', '3');
        b.makeMove('a', '1', 'a', '2');
        b.makeMove('e', '5', 'e', '4');
        b.getMoves(moves);
        Move rMove = Move.move('a', '2', 'b', '2');
        assertEquals(true, b.legalMove(rMove));
    }


    @Test
    public void testCorrectExtendedJumps() {
        Board b = new Board();
        ArrayList<Move> moves = new ArrayList<Move>();
        String init4 = "w---- b---- -b-b- -b-b- -----";
        b.setPieces(init4, WHITE);
        b.getMoves(moves);

        assertEquals(true, b.legalMove(Move.parseMove("a1-a3-c3-e3-c5-a3")));
    }
}

