package restaurantrush;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import restaurantrush.boundary.RestaurantCLI;

/**
 * Entry point. With no arguments the manager types the choices. With "--input choices.txt" the
 * choices are read from a file instead (and echoed), which replays a recorded run.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            new RestaurantCLI(System.in, System.out, false).start();
        } else if (args.length == 2 && args[0].equals("--input")) {
            try (InputStream choices = new FileInputStream(args[1])) {
                new RestaurantCLI(choices, System.out, true).start();
            } catch (IOException unreadable) {
                System.err.println("Cannot read the choices file '" + args[1] + "': " + unreadable.getMessage());
                System.exit(1);
            }
        } else {
            System.err.println("Usage: java -cp out restaurantrush.Main [--input choices.txt]");
            System.exit(2);
        }
    }
}
