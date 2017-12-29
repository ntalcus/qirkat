package qirkat;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Observable;
import java.util.Observer;

import static qirkat.PieceColor.*;
import static qirkat.Move.*;

/** A Qirkat board.   The squares are labeled by column (a char value between
 *  'a' and 'e') and row (a char value between '1' and '5'.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (with row 0 being the bottom row)
 *  counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Noah "broke the stylecheck" Alcus
 */
class Board extends Observable {

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new PieceColor[5][5];
        _lArray = new boolean[BOARDSIZE];
        _rArray = new boolean[BOARDSIZE];
        clear();
        _lastState = null;
    }

    /** Datatype representing the Board. */
    private PieceColor[][] _board;

    /** Datstructure representing left moves. */
    private boolean[] _lArray;

    /** Datastruct representing right moves. */
    private boolean[] _rArray;

    /** Returns if left move is possible for piece at K. */
    private boolean lPoss(int k) {
        return _lArray[k];
    }

    /** Sets left move array to reflect a left move MOV. */
    public void logMoveLeft(Move mov) {
        assert mov.isLeftMove();
        _rArray[index(mov.col1(), mov.row1())] = false;
    }

    /** Returns if right move is possible for piece at K. */
    private boolean rPoss(int k) {
        return _rArray[k];
    }

    /** Sets right move array to reflect a right move MOV. */
    public void logMoveRight(Move mov) {
        assert mov.isRightMove();
        _lArray[index(mov.col1(), mov.row1())] = false;
    }

    /** Resets the left moves arrays at position k after
     * a jump/right move MOV. */
    public void resetLMoves(Move mov) {
        int k = index(mov.col1(), mov.row1());
        _lArray[k] = true;
    }

    /** Resets the right moves arrays at position k
     *  after a jump/left move MOV. */
    public void resetRMoves(Move mov) {
        int k = index(mov.col1(), mov.row1());
        _rArray[k] = true;
    }

    /** A copy of B. */
    Board(Board b) {
        _board = new PieceColor[5][5];
        _gameOver = false;
        _lastState = null;
        _lArray = new boolean[BOARDSIZE];
        _rArray = new boolean[BOARDSIZE];
        for (int i = 0; i < BOARDSIZE; i++) {
            _lArray[i] = _rArray[i] = true;
        }
        internalCopy(b);
    }

    /** Return a constant view of me (allows any access method, but no
     *  method that modifies it). */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions. */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;
        _lastState = null;
        for (int i = 0; i < BOARDSIZE; i++) {
            _lArray[i] = _rArray[i] = true;
        }

        setPieces("wwwwwwwwwwbb-wwbbbbbbbbbb", _whoseMove);

        setChanged();
        notifyObservers();
    }

    /** Copy B into me. */
    void copy(Board b) {
        internalCopy(b);
    }

    /** Copy B into me. */
    private void internalCopy(Board b) {
        int linLength = BOARDSIZE;
        for (int i = 0; i < linLength; i += 1) {
            set(i, b.get(i));
            _lArray[i] = b._lArray[i];
            _rArray[i] = b._rArray[i];
            _gameOver = b._gameOver;
        }
        if (b.getLastState() != null) {
            _lastState = b.getLastState();
        }
        _whoseMove = b.whoseMove();
    }

    /** Set my contents as defined by STR.  STR consists of 25 characters,
     *  each of which is b, w, or -, optionally interspersed with whitespace.
     *  These give the contents of the Board in row-major order, starting
     *  with the bottom row (row 1) and left column (column a). All squares
     *  are initialized to allow horizontal movement in either direction.
     *  NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }
        _whoseMove = nextMove;
        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b':
            case 'B':
                set(k, BLACK);
                break;
            case 'w':
            case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
            _lArray[k] = true;
            _rArray[k] = true;
        }
        if (getMoves().size() == 0) {
            _gameOver = true;
        }
        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if the current player has
     *  no moves. */
    boolean gameOver() {
        return _gameOver;
    }

    /** Return the current contents of square C R, where 'a' <= C <= 'e',
     *  and '1' <= R <= '5'.  */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);
        return get(index(c, r));
    }

    /** Return the current contents of the square at linearized index K. */
    PieceColor get(int k) {
        assert validSquare(k);
        PieceColor item = _board[k % 5][k / 5];
        return item;
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'e', and
     *  '1' <= R <= '5'. */
    private void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        set(index(c, r), v);
    }

    /** Set get(K) to V, where K is the linearized index of a square. */
    private void set(int k, PieceColor v) {
        assert validSquare(k);
        _board[k % 5][k / 5] = v;
    }

    /** Return true iff MOV is legal on the current board. */
    boolean legalMove(Move mov) {
        ArrayList<Move> moves = new ArrayList<Move>();
        getMoves(moves);
        if (mov.isVestigial()) {
            return false;
        }
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i) == mov) {
                return true;
            }
        }
        return false;
    }

    /** Return a list of all legal moves from the current position. */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        return result;
    }

    /** Add all legal moves from the current position to MOVES. */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {
            return;
        }
        if (jumpPossible()) {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        } else {
            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getMoves(moves, k);
            }
        }
    }

    /** Add all legal non-capturing moves from the position
     *  with linearized index K to MOVES. */
    private void getMoves(ArrayList<Move> moves, int k) {

        PieceColor cPiece = get(k);
        if (cPiece != whoseMove()) {
            return;
        }
        int min, max;
        if (cPiece == WHITE) {
            if (MAXWHITE <= k) {
                return;
            }
            min = 0;
            max = 1;
        } else if (cPiece == BLACK) {
            if (k <= MINBLACK) {
                return;
            }
            min = -1;
            max = 0;
        } else {
            return;
        }
        for (int rMove = min; rMove <= max; rMove += 1) {

            for (int cMove = -1; cMove <= 1; cMove++) {

                int linInd = k + rMove * 5 + cMove;

                if (Move.validSquare(linInd)
                        && k % 5 + cMove >= 0 && k % 5 + cMove <= 4
                        && get(linInd) == EMPTY) {

                    if (rMove != 0 && cMove != 0 && k % 2 != 0) {
                        int idkwtftodo;
                    } else if (rMove == 0 && cMove == -1 && !lPoss(k)) {
                        int idkwtftodo;
                    } else if (rMove == 0 && cMove == 1 && !rPoss(k)) {
                        int idkwtftodo;
                    } else {
                        char col0 = Move.col(k), row0 = Move.row(k),
                                col1 = Move.col(linInd),
                                row2 = Move.row(linInd);
                        moves.add(Move.move(col0, row0, col1, row2));
                    }
                }
            }
        }
    }


    /** Add all legal captures from the position with linearized index K
     *  to MOVES. */
    private void getJumps(ArrayList<Move> moves, int k) {
        PieceColor cPiece = get(k);
        if (cPiece != whoseMove()) {
            return;
        }
        int min, max;
        if (cPiece.isPiece()) {
            min = -2;
            max = 2;
        } else {
            return;
        }
        for (int rMove = min; rMove <= max; rMove += 2) {
            for (int cMove = -2; cMove <= 2; cMove += 2) {
                int linInd = k + rMove * 5 + cMove;
                if (Move.validSquare(linInd)
                        && k % 5 + cMove >= 0 && k % 5 + cMove <= 4
                        && get(linInd) == EMPTY
                        && get((linInd + k) / 2) == cPiece.opposite()) {
                    if (rMove != 0 && cMove != 0 && k % 2 != 0) {
                        int idkwhattodohere;
                    } else {
                        char col0 = Move.col(k), row0 = Move.row(k),
                                col1 = Move.col(linInd),
                                row1 = Move.row(linInd);
                        ArrayList<Move> cont = jumperHelper(col0, row0,
                                col1, row1, cPiece);
                        if (cont.size() > 0) {
                            for (Move move : cont) {
                                moves.add(Move.move(col0, row0, col1,
                                        row1, move));
                            }
                        } else {
                            moves.add(Move.move(col0, row0, col1, row1));
                        }
                    }
                }
            }
        }
    }

    /** takes beginng loc BCOL-BROW and end loc DCOL-DROW and
     * jumping piececolor CP and returns an ArrayList of possible
     * jump continuations. */
    public ArrayList<Move> jumperHelper(char bCol, char bRow,
                                         char dCol, char dRow, PieceColor cp) {
        ArrayList<Move> nextJumps = new ArrayList<Move>();
        set(dCol, dRow, cp);
        set(bCol, bRow, EMPTY);
        set((char) ((bCol + dCol) / 2), (char) ((bRow + dRow) / 2), EMPTY);


        getJumps(nextJumps, index(dCol, dRow));

        set(dCol, dRow, EMPTY);
        set(bCol, bRow, cp);
        set((char) ((bCol + dCol) / 2),
                (char) ((bRow + dRow) / 2), cp.opposite());
        return nextJumps;
    }

    /** Return true iff PMOV is a valid jump sequence on the current board.
     *  PMOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     *  could be continued and are valid as far as they go.  */
    boolean checkJump(Move pMov, boolean allowPartial) {
        if (pMov == null) {
            return true;
        } else {
            ArrayList<Move> allMoves = new ArrayList<Move>();
            getMoves(allMoves);
            if (allMoves.size() == 0) {
                return false;
            }
            if (!allMoves.get(0).isJump()) {
                return false;
            }
            for (Move move : allMoves) {
                if (move == pMov) {
                    return true;
                }
            }
            if (allowPartial) {
                Move pPointer;
                Move mPointer;
                for (Move move: allMoves) {
                    if (move.col0() != pMov.col0()
                            || move.row0() != pMov.row0()
                            || move.col1() != pMov.col1()
                            || move.row1() != pMov.row1()) {
                        break;
                    }
                    pPointer = pMov;
                    mPointer = move;
                    while (pPointer != null) {
                        pPointer = pPointer.jumpTail();
                        mPointer = mPointer.jumpTail();
                        if (mPointer == null) {
                            break;
                        }
                        if (move.col1() != pMov.col1()
                                || move.row1() != pMov.row1()) {
                            break;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true iff a jump is possible for a piece at position C R. */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /** Return true iff a jump is possible for a piece at position with
     *  linearized index K. */
    boolean jumpPossible(int k) {
        ArrayList<Move> poss = new ArrayList<Move>();
        getJumps(poss, k);
        return poss.size() != 0;
    }

    /** Return true iff a jump is possible from the current board. */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null));
    }

    /** Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     *  Assumes the result is legal. */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next));
    }

    /** Make the Move MOV on this Board, assuming it is legal. */
    void makeMove(Move mov) {
        boolean test1 = legalMove(mov);
        if (!legalMove(mov)) {
            throw new IllegalArgumentException("illegal move");
        }
        PieceColor currColor = get(mov.col0(), mov.row0());
        Board lastState = new Board();
        lastState.internalCopy(this);
        _lastState = lastState;
        _whoseMove = _whoseMove.opposite();
        Move currMove = mov;
        if (mov.isJump()) {
            Move currJump = currMove;
            boolean jumpin = true;
            while (jumpin) {
                jumpin = false;
                set(currJump.col0(), currJump.row0(), EMPTY);
                set(currJump.jumpedCol(), currJump.jumpedRow(), EMPTY);
                set(currJump.col1(), currJump.row1(), currColor);
                if (currJump.jumpTail() != null) {
                    jumpin = true;
                    currJump = currJump.jumpTail();
                } else {
                    resetLMoves(currJump);
                    resetRMoves(currJump);
                }
            }
        } else {
            set(currMove.col0(), currMove.row0(), EMPTY);
            set(currMove.col1(), currMove.row1(), currColor);
            if (currMove.isLeftMove()) {
                logMoveLeft(currMove);
                resetLMoves(currMove);
            } else if (currMove.isRightMove()) {
                logMoveRight(currMove);
                resetRMoves(currMove);
            } else {
                resetLMoves(currMove);
                resetRMoves(currMove);
            }
        }
        if (!(getMoves().size() > 0)) {
            _gameOver = true;
        }
        setChanged();
        notifyObservers();
    }

    /** Undo the last move, if any. */
    void undo() {
        if (_lastState == null) {
            return;
        } else {
            internalCopy(_lastState);
        }
        setChanged();
        notifyObservers();
    }


    @Override
    public boolean equals(Object object) {
        if (object instanceof Board) {
            Board boardCast = (Board) object;
            String bStr = boardCast.toString();
            String bStr0 = toString();
            PieceColor bWhoseMove = boardCast.whoseMove();
            return (bStr.equals(toString())
                    && _whoseMove == boardCast.whoseMove());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /** Return a text depiction of the board.  If LEGEND, supply row and
     *  column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        boolean yessir = false;
        if (legend) {
            out.format("1 2 3 4 5\n");
        }
        String col1, col2, col3, col4, col5, leg;
        leg = "";
        for (int i = 4; i >= 0; i--) {
            if (legend) {
                out.format("1 2 3 4 5\n");
                leg = new String[]{"a", "b", "c", "d", "e"}[i];
                yessir = true;
            }
            col1 = get(i * 5).shortName();
            col2 = get(i * 5 + 1).shortName();
            col3 = get(i * 5 + 2).shortName();
            col4 = get(i * 5 + 3).shortName();
            col5 = get(i * 5 + 4).shortName();
            out.format("  %s%s %s %s %s %s%s",
                       leg, col1, col2, col3, col4, col5, leg);
            if (i != 0) {
                out.format("\n");
            }
        }
        if (yessir) {
            out.format("/n  1 2 3 4 5");
        }
        return out.toString();
    }

    /** Return true iff there is a move for the current player. */
    private boolean isMove() {
        return false;
    }

    /** Size of Board. */
    private static final int BOARDSIZE = 25;

    /** Max moveable white index. */
    private static final int MAXWHITE = 20;

    /** Min moveable white index. */
    private static final int MINBLACK = 4;

    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Set true when game ends. */
    private boolean _gameOver;

    /** Sets a last state for undo to revert to. */
    private Board _lastState;

    /** Returns a board's last state. */
    public Board getLastState() {
        return _lastState;
    }

    /** Convenience value giving values of pieces at each ordinal position. */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /** One cannot create arrays of ArrayList<Move>, so we introduce
     *  a specialized private list type for this purpose. */
    private static class MoveList extends ArrayList<Move> {
    }

    /** A read-only view of a Board. */
    private class ConstantBoard extends Board implements Observer {
        /** A constant view of this Board. */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move) {
            assert false;
        }

        /** Undo the last move. */
        @Override
        void undo() {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }
}
