package qirkat;

import java.util.ArrayList;

import static qirkat.PieceColor.*;

/** A Player that computes its own moves.
 *  @author Noah "submits at 11:57pm, style checks, submits at 11:59pm" Alcus
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private int MAX_DEPTH = 20;
    /** A position magnitude indicating a win (for white if positive, black
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor, int _maxDepth) {
        super(game, myColor);
        MAX_DEPTH = _maxDepth;
    }

    @Override
    Move myMove() {

        Main.startTiming();
        Move move = findMove();
        Main.endTiming();
        if (move == null) {
            board().gameOver();
        }
        return move;
    }

    @Override
    boolean isAI() {
        return true;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        Move best;
        best = null;

        int score;
        if (this.myColor() == WHITE) {
            score = alpha;
        } else {
            score = beta;
        }
        ArrayList<Move> moves = board.getMoves();
        for (Move move : moves) {
            board.makeMove(move);
            if (sense == 1) {
                score = minimizer(board, depth - 1, alpha, beta, -sense);
            } else {
                score = maximizer(board, depth - 1, alpha, beta, -sense);
            }
            board.undo();
            if (myColor() == WHITE) {
                if (score > alpha) {
                    alpha = score;
                    best = move;
                }
            } else {
                if (score < beta) {
                    beta = score;
                    best = move;
                }
            }
            if (alpha >= INFTY) {
                break;
            }
        }

        if (saveMove) {
            _lastFoundMove = best;
        }

        return score;
    }

    /** Returns int representing best result at minimum based on
     * current BOARD evaluating to DEPTH (then using static valuation)
     * utilizing ALPHA-BETA pruning and common SENSE. */
    private int maximizer(Board board, int depth,
                           int alpha, int beta, int sense) {
        if (board.gameOver()) {
            if (board.whoseMove() == WHITE) {
                return -WINNING_VALUE;
            } else {
                return WINNING_VALUE;
            }
        }
        if (depth <= 0) {
            return staticScore(board);
        }
        ArrayList<Move> moves = board.getMoves();
        for (Move move:moves) {
            board.makeMove(move);
            int score = minimizer(board, depth - 1, alpha, beta, -sense);
            board.undo();
            alpha = java.lang.Math.max(alpha, score);
            if (alpha >= beta) {
                return beta;
            }
        }
        return alpha;

    }

    /** Does anyone really look at these? This bad boy is the
     * minimizer function for black pieces. it uses BOARD
     * DEPTH ALPHA BETA SENSE in the same way as the
     * other function but to minimize. it
     * returns an int that gives the score of the most good
     * result for BLACK:praiseemoji: */
    private int minimizer(Board board,
                          int depth, int alpha, int beta, int sense) {
        if (board.gameOver()) {
            if (board.whoseMove() == WHITE) {
                return -WINNING_VALUE;
            } else {
                return WINNING_VALUE;
            }
        }
        if (depth <= 0) {
            return staticScore(board);
        }
        ArrayList<Move> moves = board.getMoves();
        for (Move move:moves) {
            boolean daBugger = false;
            try {
                board.makeMove(move);
            } catch (IllegalArgumentException I) {
                daBugger = true;
            }
            if (daBugger) {
                board.makeMove(move);
            }
            int score = maximizer(board, depth - 1, alpha, beta, -sense);
            board.undo();
            beta = java.lang.Math.min(beta, score);
            if (alpha >= beta) {
                return alpha;
            }
        }
        return beta;
    }


    /** Return a heuristic value for BOARD. Any
     *  call to this should be multiplied by sense. */
    private int staticScore(Board board) {
        int score;
        int wPieces = 0;
        int bPieces = 0;
        for (int i = 0; i < BSIZE; i++) {
            PieceColor piece = board.get(i);
            if (piece == EMPTY) {
                break;
            } else if (piece == WHITE) {
                wPieces += 1;
            } else if (piece == BLACK) {
                bPieces += 1;
            }
        }
        score = (wPieces - bPieces);
        if (bPieces == 0) {
            score = WINNING_VALUE;
        } else if (wPieces == 0) {
            score = WINNING_VALUE * -1;
        }
        return score;
    }

    /** Board size. */
    private static final int BSIZE = 25;
}
