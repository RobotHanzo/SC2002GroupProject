package restaurantrush;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import restaurantrush.boundary.RestaurantCLI;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.MenuItem;
import restaurantrush.entity.Order;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Table;

/**
 * Plain-Java checks (no test framework needed). Run from the project root:
 *   javac --release 17 -d out -sourcepath src test/restaurantrush/GameTests.java
 *   java -cp out restaurantrush.GameTests
 * Exits with status 1 if any check fails.
 */
public final class GameTests {
    private static int passed;
    private static int failed;

    private GameTests() {
    }

    public static void main(String[] args) throws IOException {
        menuMatchesTheBrief();
        orderMovesThroughLegalStatesOnly();
        customerLosesEightPerWaitingTurnAndNothingOnceServed();
        customerTurnGuardsDelayEatingAndPayment();
        customerChoosesFoodByIdRule();
        tableHoldsOneCustomer();
        paymentRequiresReadyToPayAndFreesTable();
        abandonmentCancelsOrderFreesTableAndLeavesQueue();
        victoryRunIsReproducible();
        defeatRunIsReproducible();
        invalidChoicesDoNotUseUpTheTask();
        System.out.println(passed + " checks passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---- entity checks -------------------------------------------------------------------

    private static void menuMatchesTheBrief() {
        List<MenuItem> menu = new Restaurant().getMenu().getItems();
        check("menu has three items", menu.size() == 3);
        check("Salad $9.00, 1 unit", item(menu, "Salad", 900, 1));
        check("Burger $15.00, 1 unit", item(menu, "Burger", 1500, 1));
        check("Pasta $18.00, 2 units", item(menu, "Pasta", 1800, 2));
    }

    private static void orderMovesThroughLegalStatesOnly() {
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        Order order = new Order(pasta);
        check("new order is PLACED", order.getStatus() == OrderStatus.PLACED);
        expectRejected("a PLACED order cannot be served", order::markServed);
        order.advancePreparation();
        check("first of two units: PREPARING", order.getStatus() == OrderStatus.PREPARING);
        order.advancePreparation();
        check("last unit: READY", order.getStatus() == OrderStatus.READY && order.isReady());
        expectRejected("a READY order cannot be prepared further", order::advancePreparation);
        order.markServed();
        order.markPaid();
        check("served then paid: PAID", order.getStatus() == OrderStatus.PAID);
        expectRejected("a PAID order cannot be cancelled", order::cancel);

        Order burger = new Order(new MenuItem("Burger", 1500, 1));
        burger.advancePreparation();
        check("a 1-unit dish goes PLACED to READY", burger.isReady());
    }

    private static void customerLosesEightPerWaitingTurnAndNothingOnceServed() {
        Customer waiting = new Customer(1);
        waiting.reduceSatisfaction();
        check("waiting customer loses 8", waiting.getSatisfaction() == 92);
        waiting.markSeated();
        waiting.reduceSatisfaction();
        check("seated customer loses 8", waiting.getSatisfaction() == 84);
        waiting.assignOrder(new Order(new MenuItem("Burger", 1500, 1)));
        waiting.reduceSatisfaction();
        check("customer with a placed order still loses 8", waiting.getSatisfaction() == 76);
        waiting.markServed(5);
        waiting.reduceSatisfaction();
        check("served customer loses nothing", waiting.getSatisfaction() == 76);

        Customer patient = new Customer(2);
        for (int turn = 1; turn <= 12; turn++) {
            patient.reduceSatisfaction();
        }
        check("after 12 waiting turns the customer has 4 left and stays", patient.getSatisfaction() == 4 && !patient.shouldAbandon());
        patient.reduceSatisfaction();
        check("satisfaction bottoms out at 0, then the customer abandons", patient.getSatisfaction() == 0 && patient.shouldAbandon());
    }

    private static void customerTurnGuardsDelayEatingAndPayment() {
        Customer customer = new Customer(1);
        customer.markSeated();
        customer.assignOrder(new Order(new MenuItem("Burger", 1500, 1)));
        customer.markServed(4);
        check("cannot finish eating on the serving turn", !customer.canFinishEating(4));
        check("can finish eating a turn later", customer.canFinishEating(5));
        customer.markReadyToPay(5);
        check("cannot pay on the READY_TO_PAY turn", !customer.canPay(5));
        check("can pay a turn later", customer.canPay(6));
        expectRejected("marking READY_TO_PAY twice is rejected", () -> customer.markReadyToPay(6));
    }

    private static void customerChoosesFoodByIdRule() {
        List<MenuItem> menu = new Restaurant().getMenu().getItems();
        check("customer 1 picks Burger", new Customer(1).chooseMenuItem(menu).getName().equals("Burger"));
        check("customer 2 picks Pasta", new Customer(2).chooseMenuItem(menu).getName().equals("Pasta"));
        check("customer 3 picks Salad", new Customer(3).chooseMenuItem(menu).getName().equals("Salad"));
    }

    private static void tableHoldsOneCustomer() {
        Table table = new Table(1);
        Customer first = new Customer(1);
        table.seat(first);
        check("seated table is not available", !table.isAvailable() && table.getOccupied() == first);
        expectRejected("a second customer cannot take an occupied table", () -> table.seat(new Customer(2)));
        table.release();
        check("released table is available", table.isAvailable());
    }

    private static void paymentRequiresReadyToPayAndFreesTable() {
        Restaurant restaurant = new Restaurant();
        Customer customer = restaurant.registerArrival();
        Table table = restaurant.findAvailableTable();
        restaurant.getWaiter().seatCustomer(customer, table);
        restaurant.getWaitingQueue().remove(customer);
        expectRejected("the Cashier cannot charge a seated customer",
                () -> restaurant.getCashier().processPayment(customer, restaurant));
        restaurant.getWaiter().takeOrder(customer, customer.chooseMenuItem(restaurant.getMenu().getItems()));
        restaurant.getChef().prepare(customer.getOrder());
        restaurant.getWaiter().serveOrder(customer, 3);
        customer.markReadyToPay(4);
        restaurant.getCashier().processPayment(customer, restaurant);
        check("payment adds the bill to revenue", restaurant.getRevenueCents() == 1500);
        check("customer and order are PAID", customer.getStatus() == CustomerStatus.PAID
                && customer.getOrder().getStatus() == OrderStatus.PAID);
        check("the table is free again", table.isAvailable());
        check("average satisfaction counts the paid customer", restaurant.getAverageSatisfaction() == 100.0);
        expectRejected("paying twice is rejected", () -> restaurant.getCashier().processPayment(customer, restaurant));
        check("revenue is unchanged after the rejected second payment", restaurant.getRevenueCents() == 1500);
    }

    private static void abandonmentCancelsOrderFreesTableAndLeavesQueue() {
        Restaurant restaurant = new Restaurant();
        Customer seated = restaurant.registerArrival();
        Customer queued = restaurant.registerArrival();
        Table table = restaurant.findAvailableTable();
        restaurant.getWaiter().seatCustomer(seated, table);
        restaurant.getWaitingQueue().remove(seated);
        restaurant.getWaiter().takeOrder(seated, seated.chooseMenuItem(restaurant.getMenu().getItems()));
        restaurant.handleAbandonment(seated);
        check("abandoned customer is ABANDONED", seated.getStatus() == CustomerStatus.ABANDONED);
        check("abandoned customer's order is CANCELLED", seated.getOrder().getStatus() == OrderStatus.CANCELLED);
        check("abandoned customer's table is free", table.isAvailable());
        restaurant.handleAbandonment(queued);
        check("a queued customer who leaves is removed from the queue", restaurant.getWaitingQueue().isEmpty());
        check("two unhappy departures are counted", restaurant.getUnhappyDepartureCount() == 2);
        check("abandoned customers add no revenue", restaurant.getRevenueCents() == 0);
    }

    // ---- full-game checks: replay the recorded inputs in runs/ ---------------------------

    private static void victoryRunIsReproducible() throws IOException {
        String transcript = play("runs/victory.txt");
        check("victory run ends in VICTORY", transcript.contains("\nVICTORY\n"));
        check("victory run: 4 paid", transcript.contains("Paid customers: 4 (need 4)"));
        check("victory run: $57.00", transcript.contains("Revenue: $57.00 (need $45.00)"));
        check("victory run: average 78.0", transcript.contains("Average satisfaction of paid customers: 78.0"));
        check("the game ran all 18 turns", transcript.contains("Turns used: 18"));
        check("exactly six customers arrive in the base setting", transcript.contains("  #6 ") && !transcript.contains("  #7 "));
    }

    private static void defeatRunIsReproducible() throws IOException {
        String transcript = play("runs/defeat.txt");
        check("defeat run ends in DEFEAT", transcript.contains("\nDEFEAT\n"));
        check("defeat run: nobody paid, revenue $0.00", transcript.contains("Paid customers: 0")
                && transcript.contains("Revenue: $0.00 (need"));
        check("defeat run: average shows N/A", transcript.contains("Average satisfaction of paid customers: N/A (need"));
        check("defeat run: two customers walked out", transcript.contains("Unhappy departures: 2"));
    }

    private static void invalidChoicesDoNotUseUpTheTask() throws IOException {
        String transcript = play("runs/victory.txt");
        check("an unknown customer is explained", transcript.contains("There is no customer #9"));
        check("serving a dish that is not READY is explained", transcript.contains("is not READY yet"));
        check("a rejected choice is followed by another prompt in the same turn",
                transcript.contains("Choose again.\nWaiter  [SEAT <#> | TAKE_ORDER <#> | SERVE <#> | WAIT]: ")
                        || transcript.contains("Choose again.\nChef    [COOK <#> | WAIT]: "));
    }

    // ---- tiny helpers --------------------------------------------------------------------

    private static String play(String inputFile) throws IOException {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (FileInputStream choices = new FileInputStream(inputFile);
                PrintStream out = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            new RestaurantCLI(choices, out, true).start();
        }
        return captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static boolean item(List<MenuItem> menu, String name, int priceCents, int units) {
        for (MenuItem candidate : menu) {
            if (candidate.getName().equals(name)) {
                return candidate.getPriceCents() == priceCents && candidate.getPreparationUnits() == units;
            }
        }
        return false;
    }

    private static void expectRejected(String name, Runnable action) {
        try {
            action.run();
            check(name, false);
        } catch (IllegalStateException | IllegalArgumentException expected) {
            check(name, true);
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("FAILED: " + name);
        }
    }
}
