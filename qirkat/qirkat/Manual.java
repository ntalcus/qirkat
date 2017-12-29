package qirkat;

import static qirkat.PieceColor.*;
import static qirkat.Command.Type.*;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Noah Alcus
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
        _prompt = myColor + ": ";
    }

    @Override
    Move myMove() {
        Move mov;
        Command cmd = this.game().getMoveCmnd(_prompt);
        if (cmd == null) {
            return null;
        }
        mov = Move.parseMove(cmd.operands()[0]);
        return mov;
    }

    /** Identifies the player serving as a source of input commands. */
    private String _prompt;
}

