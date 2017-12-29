package qirkat;

/* Author: P. N. Hilfinger */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import static qirkat.PieceColor.*;
import static qirkat.Game.State.*;
import static qirkat.Command.Type.*;
import static qirkat.GameException.error;

/** Controls the play of the game.
 *  @author Noah "ok at coding" Alcus
 */
class Game {

    /** States of play. */
    static enum State {
        SETUP, PLAYING;
    }

    /** A new Game, using BOARD to play on, reading initially from
     *  BASESOURCE and using REPORTER for error and informational messages. */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _constBoard = _board.constantView();
        _reporter = reporter;
    }

    /** Run a session of Qirkat gaming. */
    void process() {
        Player black, white;
        doClear(null);
        while (true) {
            while (_state == SETUP) {
                try {
                    doCommand();
                } catch (IllegalArgumentException I) {
                    reportError("that move is illegal.");
                }
            }
            black = createBlack();
            white = createWhite();

            if (_board.gameOver()) {
                gameWinnah = _board.whoseMove().opposite();
            }
            Player currPlayer;
            if (_board.whoseMove() == WHITE) {
                currPlayer = white;
            } else {
                currPlayer = black;
            }
            while (_state != SETUP && !_board.gameOver()) {
                Move move;
                move = currPlayer.myMove();
                if (move == null) {
                    _state = SETUP;
                    break;
                }
                if (_state == PLAYING) {
                    try {
                        _board.makeMove(move);
                        doDump(new String[]{});
                        if (currPlayer.isAI()) {
                            PieceColor color = currPlayer.myColor();
                            String moveStr = move.toString();
                            reportMove("%s moves %s.", color, moveStr);
                        }
                    } catch (IllegalArgumentException I) {
                        reportError("that move is illegal.");
                    }
                }
                if (_board.gameOver()) {
                    gameWinnah = _board.whoseMove().opposite();
                }
                if (_board.whoseMove() == WHITE) {
                    currPlayer = white;
                } else {
                    currPlayer = black;
                }
            }
            if (_state == PLAYING) {
                reportWinner();
            }
            _state = SETUP;
        }
    }

    /** Creates and returns black player. */
    private Player createBlack() {
        if (_blackIsManual) {
            return new Manual(this, BLACK);
        } else {
            return new AI(this, BLACK, 12);
        }
    }

    /** Creates and returns white player. */
    private Player createWhite() {
        if (_whiteIsManual) {
            return new Manual(this, WHITE);
        } else {
            return new AI(this, WHITE, 6);
        }
    }

    /** Return a read-only view of my game board. */
    Board board() {
        return _constBoard;
    }

    /** Perform the next command from our input source. */
    void doCommand() {
        try {
            Command cmnd =
                Command.parseCommand(_inputs.getLine("qirkat: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /** Read and execute commands until encountering a move or until
     *  the game leaves playing state due to one of the commands. Return
     *  the terminating move command, or null if the game first drops out
     *  of playing mode. If appropriate to the current input source, use
     *  PROMPT to prompt for input. */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                switch (cmnd.commandType()) {
                case PIECEMOVE:
                    return cmnd;
                default:
                    _commands.get(cmnd.commandType()).accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /** Return random integer between 0 (inclusive) and MAX>0 (exclusive). */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /** Report a move, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /** Report an error, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /** Perform the command 'auto OPERANDS[0]'. */
    void doAuto(String[] operands) {
        _state = SETUP;
        if (operands[0].toLowerCase().equals("white")) {
            _whiteIsManual = false;
        } else if (operands[0].toLowerCase().equals("black")) {
            _blackIsManual = false;
        } else {
            doError(operands);
        }
    }

    /** Perform a 'help' command. */
    void doHelp(String[] unused) {
        InputStream helpIn =
            Game.class.getClassLoader().getResourceAsStream("qirkat/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                    = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /** Perform the command 'load OPERANDS[0]'. */
    void doLoad(String[] operands) {
        doClear(operands);
        try {
            FileReader reader = new FileReader(operands[0]);
            _inputs.addSource(new ReaderSource(reader, false));
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /** Perform the command 'manual OPERANDS[0]'. */
    void doManual(String[] operands) {
        _state = SETUP;
        if (operands[0].toLowerCase().equals("white")) {
            _whiteIsManual = true;
        } else if (operands[0].toLowerCase().equals("black")) {
            _blackIsManual = true;
        } else {
            doError(operands);
        }
    }

    /** Exit the program. */
    void doQuit(String[] unused) {
        Main.reportTotalTimes();
        System.exit(0);
    }

    /** Perform the command 'start'. */
    void doStart(String[] unused) {
        _state = PLAYING;
    }

    /** Perform the move OPERANDS[0]. */
    void doMove(String[] operands) {
        Move move = Move.parseMove(operands[0]);
        _board.makeMove(move);
    }

    /** Perform the command 'clear'. */
    void doClear(String[] unused) {
        _whiteIsManual = true;
        _blackIsManual = false;
        _state = SETUP;
        _board.clear();
    }

    /** Perform the command 'set OPERANDS[0] OPERANDS[1]'. */
    void doSet(String[] operands) {
        PieceColor pc = null;
        if (operands[0].toLowerCase().equals("white")) {
            pc = WHITE;
        } else if (operands[0].toLowerCase().equals("black")) {
            pc = BLACK;
        } else {
            doError(operands);
        }
        _board.clear();
        _board.setPieces(operands[1], pc);
    }

    /** Perform the command 'dump'. */
    void doDump(String[] unused) {
        _reporter.moveMsg("===\n%s\n===", _board);
    }

    /** Execute 'seed OPERANDS[0]' command, where the operand is a string
     *  of decimal digits. Silently substitutes another value if
     *  too large. */
    void doSeed(String[] operands) {
        try {
            _randoms.setSeed(Long.parseLong(operands[0]));
        } catch (NumberFormatException e) {
            _randoms.setSeed(Long.MAX_VALUE);
        }
    }

    /** Execute the artificial 'error' command. */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /** Report the outcome of the current game. */
    void reportWinner() {
        String msg;
        msg = "%s wins.";
        _reporter.outcomeMsg(msg, gameWinnah.toString());
    }

    /** Mapping of command types to methods that process them. */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
        new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(SETBOARD, this::doSet);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }

    /** Input source. */
    private final CommandSources _inputs = new CommandSources();

    /** My board and its read-only view. */
    private Board _board, _constBoard;
    /** Indicate which players are manual players (as opposed to AIs). */
    private boolean _whiteIsManual, _blackIsManual;
    /** Current game state. */
    private State _state;
    /** Used to send messages to the user. */
    private Reporter _reporter;
    /** Source of pseudo-random numbers (used by AIs). */
    private Random _randoms = new Random();
    /** This is the game winnah. */
    private PieceColor gameWinnah;
}
