package restaurantrush;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;

import restaurantrush.boundary.RestaurantCLI;
import restaurantrush.control.GameController;

/**
 * Entry point, and the one place that wires the command-line boundary to the controller. With no
 * arguments the manager types the choices. With "--input choices.txt" the choices are read from a
 * file instead (and echoed), which replays a recorded run.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            play(System.in, System.out, false);
        } else if (args.length == 2 && args[0].equals("--input")) {
            try (InputStream choices = new FileInputStream(args[1])) {
                play(choices, System.out, true);
            } catch (IOException unreadable) {
                System.err.println("Cannot read the choices file '" + args[1] + "': " + unreadable.getMessage());
                System.exit(1);
            }
        } else {
            System.err.println("Usage: java -cp out restaurantrush.Main [--input choices.txt]");
            System.exit(2);
        }
    }

    /**
     * Plays one game reading the manager's choices from the given stream. If the input ends before
     * the game does, the run stops with a message. echoInput repeats each line read, so a run fed
     * from a file reads like a transcript.
     */
    public static void play(InputStream in, PrintStream out, boolean echoInput) {
        RestaurantCLI cli = new RestaurantCLI(in, out, echoInput);
        try {
            new GameController(cli).startGame();
        } catch (NoSuchElementException inputEnded) {
            cli.displayInputEnded();
        }
    }
}
