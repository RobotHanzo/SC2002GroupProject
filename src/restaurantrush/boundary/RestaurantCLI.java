package restaurantrush.boundary;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.Money;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Table;

/**
 * The command-line boundary. It prints the game state and reads the manager's choices as text;
 * it contains no game rules (the staff and domain objects decide whether a choice is legal) and
 * does not know the controller: the controller calls it, and Main wires the two together.
 */
public class RestaurantCLI {
    private final Scanner input;
    private final PrintStream out;
    private final boolean echoInput;

    /** echoInput repeats each line read, so a run fed from a file (stdin) reads like a transcript. */
    public RestaurantCLI(InputStream in, PrintStream out, boolean echoInput) {
        this.input = new Scanner(in);
        this.out = out;
        this.echoInput = echoInput;
    }

    public void displayIntro(int maxTurns) {
        out.println("SC2002 Restaurant Rush - Stage 3");
        out.println("You manage a restaurant for " + maxTurns + " turns. Each turn the Waiter does one");
        out.println("task (SEAT, TAKE_ORDER or SERVE), the Host can also SEAT one queued customer, and the Chef");
        out.println("advances one order by one unit (COOK). Once per game you may switch Happy Hour on.");
        out.println("Press Enter to WAIT. Goal: at least " + Restaurant.REQUIRED_PAID_CUSTOMERS + " paid customers, revenue of at least "
                + Money.format(Restaurant.REQUIRED_REVENUE_CENTS) + ", average satisfaction of at least "
                + Restaurant.REQUIRED_AVERAGE_SATISFACTION + ".");
    }

    /** Shown when the input ends before the game does. */
    public void displayInputEnded() {
        out.println();
        out.println("The input ended before the game finished, so the run stops here.");
    }

    public String readHostChoice() {
        return readLine("Host    [SEAT <#> | WAIT]: ");
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

    public void displayTurnStatus(Restaurant restaurant, int turn, int maxTurns) {
        out.println();
        out.println("=================== TURN " + turn + " of " + maxTurns + " ===================");
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
        out.println("Host tasks:   " + hostTasksText(restaurant));
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

    public void displayResult(boolean won, Restaurant restaurant, int turnsUsed) {
        out.println();
        out.println("=================== GAME OVER ===================");
        out.println(won ? "VICTORY" : "DEFEAT");
        out.println("Paid customers: " + restaurant.getPaidCustomerCount() + " (need "
                + Restaurant.REQUIRED_PAID_CUSTOMERS + ")");
        out.println("Revenue: " + Money.format(restaurant.getRevenueCents()) + " (need "
                + Money.format(Restaurant.REQUIRED_REVENUE_CENTS) + ")");
        out.println("Average satisfaction of paid customers: " + averageText(restaurant) + " (need "
                + Restaurant.REQUIRED_AVERAGE_SATISFACTION + ")");
        out.println("Customers served: " + restaurant.getServedCustomerCount()
                + " | Unhappy departures: " + restaurant.getUnhappyDepartureCount()
                + " | Turns used: " + turnsUsed);
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

    private static String hostTasksText(Restaurant restaurant) {
        List<Customer> seatable = restaurant.getSeatableCustomers();
        return seatable.isEmpty() ? "none available (WAIT)" : "SEAT " + ids(seatable);
    }

    private static String waiterTasksText(Restaurant restaurant) {
        List<String> tasks = new ArrayList<>();
        List<Customer> seatable = restaurant.getSeatableCustomers();
        if (!seatable.isEmpty()) {
            tasks.add("SEAT " + ids(seatable));
        }
        List<Customer> toOrder = restaurant.getCustomersWithStatus(CustomerStatus.SEATED);
        if (!toOrder.isEmpty()) {
            tasks.add("TAKE_ORDER " + ids(toOrder));
        }
        List<Customer> toServe = restaurant.getCustomersToServe();
        if (!toServe.isEmpty()) {
            tasks.add("SERVE " + ids(toServe));
        }
        return tasks.isEmpty() ? "none available (WAIT)" : String.join("; ", tasks);
    }

    private static String chefTasksText(Restaurant restaurant) {
        List<Customer> toCook = restaurant.getCustomersToCook();
        return toCook.isEmpty() ? "none available (WAIT)" : "COOK " + ids(toCook);
    }

    private static String ids(List<Customer> customers) {
        List<String> numbers = new ArrayList<>();
        for (Customer customer : customers) {
            numbers.add("#" + customer.getId());
        }
        return String.join(" ", numbers);
    }
}
