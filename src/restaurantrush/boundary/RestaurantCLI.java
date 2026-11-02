package restaurantrush.boundary;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

import restaurantrush.control.GameController;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.Money;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Table;

/**
 * The command-line boundary. It prints the game state and reads the manager's choices as text;
 * it contains no game rules (the controller decides whether a choice is legal).
 */
public class RestaurantCLI {
    private final Scanner input;
    private final PrintStream out;
    private final boolean echoInput;
    private GameController controller;

    /** echoInput repeats each line read, so a run fed from a file (stdin) reads like a transcript. */
    public RestaurantCLI(InputStream in, PrintStream out, boolean echoInput) {
        this.input = new Scanner(in);
        this.out = out;
        this.echoInput = echoInput;
    }

    /** Runs one game. If the input ends before the game does, the run stops with a message. */
    public void start() {
        controller = new GameController(this);
        out.println("SC2002 Restaurant Rush - Stage 3");
        out.println("You manage a restaurant for " + controller.getMaxTurns() + " turns. Each turn the Waiter does one");
        out.println("task (SEAT, TAKE_ORDER or SERVE) and the Chef advances one order by one unit (COOK).");
        out.println("Press Enter to WAIT. Goal: at least " + GameController.REQUIRED_PAID_CUSTOMERS + " paid customers, revenue of at least "
                + Money.format(GameController.REQUIRED_REVENUE_CENTS) + ", average satisfaction of at least "
                + GameController.REQUIRED_AVERAGE_SATISFACTION + ".");
        try {
            controller.startGame();
        } catch (NoSuchElementException inputEnded) {
            out.println();
            out.println("The input ended before the game finished, so the run stops here.");
        }
    }

    public String readWaiterChoice() {
        return readLine("Waiter  [SEAT <#> | TAKE_ORDER <#> | SERVE <#> | WAIT]: ");
    }

    public String readChefChoice() {
        return readLine("Chef    [COOK <#> | WAIT]: ");
    }

    /** Asks whether to switch Happy Hour on now. Blank means no. */
    public boolean readHappyHourChoice() {
        while (true) {
            String answer = readLine("Happy Hour [y = switch on for this turn and the next | n or Enter = not now]: ")
                    .trim().toLowerCase(Locale.ROOT);
            if (answer.isEmpty() || answer.equals("n") || answer.equals("no")) {
                return false;
            }
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            displayError("'" + answer + "' is not y or n");
        }
    }

    public void displayTurnStatus(Restaurant restaurant) {
        out.println();
        out.println("=================== TURN " + controller.getCurrentTurn() + " of " + controller.getMaxTurns()
                + " ===================");
        out.println("Revenue: " + Money.format(restaurant.getRevenueCents())
                + " | Average satisfaction of paid customers: " + averageText(restaurant)
                + " | Served: " + restaurant.getServedCustomerCount()
                + " | Unhappy departures: " + restaurant.getUnhappyDepartureCount());
        out.println("Happy Hour: " + happyHourText(restaurant.getHappyHour()));
        out.println("Waiting queue: " + queueText(restaurant));
        out.println("Tables: " + tablesText(restaurant));
        out.println("Customers:");
        if (restaurant.getCustomers().isEmpty()) {
            out.println("  (nobody has arrived yet)");
        }
        for (Customer customer : restaurant.getCustomers()) {
            out.println(String.format(Locale.ROOT, "  %-4s %-8s satisfaction %3d  %-12s  order: %s",
                    "#" + customer.getId(), customer.getTypeLabel(), customer.getSatisfaction(), customer.getStatus(),
                    orderText(customer)));
        }
        out.println("Waiter tasks: " + waiterTasksText(restaurant));
        out.println("Chef tasks:   " + chefTasksText(restaurant));
    }

    public void displayMessage(String message) {
        out.println("  * " + message);
    }

    public void displayError(String message) {
        String reason = message.endsWith(".") ? message.substring(0, message.length() - 1) : message;
        out.println("  ! Not possible: " + reason + ". Choose again.");
    }

    public void displayResult(boolean won, Restaurant restaurant) {
        out.println();
        out.println("=================== GAME OVER ===================");
        out.println(won ? "VICTORY" : "DEFEAT");
        out.println("Paid customers: " + restaurant.getPaidCustomerCount() + " (need "
                + GameController.REQUIRED_PAID_CUSTOMERS + ")");
        out.println("Revenue: " + Money.format(restaurant.getRevenueCents()) + " (need "
                + Money.format(GameController.REQUIRED_REVENUE_CENTS) + ")");
        out.println("Average satisfaction of paid customers: " + averageText(restaurant) + " (need "
                + GameController.REQUIRED_AVERAGE_SATISFACTION + ")");
        out.println("Customers served: " + restaurant.getServedCustomerCount()
                + " | Unhappy departures: " + restaurant.getUnhappyDepartureCount()
                + " | Turns used: " + controller.getCurrentTurn());
    }

    private String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        String line = input.nextLine();
        if (echoInput) {
            out.println(line);
        }
        return line;
    }

    private static String averageText(Restaurant restaurant) {
        if (restaurant.getPaidCustomerCount() == 0) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.1f", restaurant.getAverageSatisfaction());
    }

    private static String happyHourText(HappyHour happyHour) {
        if (happyHour.isActive()) {
            return "ACTIVE, " + happyHour.getRemainingTurns() + " turn(s) left including this one";
        }
        return happyHour.isAvailable() ? "available (switch on once per game)" : "used";
    }

    private static String queueText(Restaurant restaurant) {
        if (restaurant.getWaitingQueue().isEmpty()) {
            return "empty";
        }
        List<String> entries = new ArrayList<>();
        for (Customer customer : restaurant.getWaitingQueue()) {
            entries.add("#" + customer.getId() + " " + customer.getTypeLabel() + " (satisfaction " + customer.getSatisfaction() + ")");
        }
        return String.join(" -> ", entries);
    }

    private static String tablesText(Restaurant restaurant) {
        List<String> entries = new ArrayList<>();
        for (Table table : restaurant.getTables()) {
            Customer occupant = table.getOccupied();
            entries.add("Table " + table.getTableId() + ": "
                    + (occupant == null ? "free"
                            : "#" + occupant.getId() + " " + occupant.getTypeLabel() + " (" + occupant.getStatus() + ")"));
        }
        return String.join(" | ", entries);
    }

    private static String orderText(Customer customer) {
        Order order = customer.getOrder();
        if (order == null) {
            return "-";
        }
        return order.getItem().getName() + " " + order.getCompletedUnits() + "/"
                + order.getItem().getPreparationUnits() + " units, " + order.getStatus()
                + ", " + Money.format(order.getBillCents());
    }

    private static String waiterTasksText(Restaurant restaurant) {
        List<String> tasks = new ArrayList<>();
        if (!restaurant.getWaitingQueue().isEmpty() && restaurant.findAvailableTable() != null) {
            tasks.add("SEAT " + ids(new ArrayList<>(restaurant.getWaitingQueue())));
        }
        List<Customer> seated = withStatus(restaurant, CustomerStatus.SEATED, false);
        if (!seated.isEmpty()) {
            tasks.add("TAKE_ORDER " + ids(seated));
        }
        List<Customer> ready = withStatus(restaurant, CustomerStatus.ORDERED, true);
        if (!ready.isEmpty()) {
            tasks.add("SERVE " + ids(ready));
        }
        return tasks.isEmpty() ? "none available (WAIT)" : String.join("; ", tasks);
    }

    private static String chefTasksText(Restaurant restaurant) {
        List<Customer> cooking = new ArrayList<>();
        for (Customer customer : withStatus(restaurant, CustomerStatus.ORDERED, false)) {
            if (!customer.getOrder().isReady()) {
                cooking.add(customer);
            }
        }
        return cooking.isEmpty() ? "none available (WAIT)" : "COOK " + ids(cooking);
    }

    /** Customers with the given status; for ORDERED, readyOnly keeps those whose dish is READY. */
    private static List<Customer> withStatus(Restaurant restaurant, CustomerStatus status, boolean readyOnly) {
        List<Customer> matching = new ArrayList<>();
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.getStatus() == status && (!readyOnly || customer.getOrder().isReady())) {
                matching.add(customer);
            }
        }
        return matching;
    }

    private static String ids(List<Customer> customers) {
        List<String> numbers = new ArrayList<>();
        for (Customer customer : customers) {
            numbers.add("#" + customer.getId());
        }
        return String.join(" ", numbers);
    }
}
