package restaurantrush;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import restaurantrush.boundary.RestaurantCLI;
import restaurantrush.control.ArrivalSchedule;
import restaurantrush.entity.CriticCustomer;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Menu;
import restaurantrush.entity.MenuItem;
import restaurantrush.entity.Order;
import restaurantrush.entity.OrderStatus;
import restaurantrush.entity.RegularCustomer;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Table;
import restaurantrush.entity.VIPCustomer;

/**
 * Plain-Java checks (no test framework needed). Run from the project root:
 *   javac --release 17 -d out -sourcepath src test/restaurantrush/GameTests.java
 *   java -cp out restaurantrush.GameTests
 * Exits with status 1 if any check fails. The first group re-checks the Stage 1 rules that must
 * still hold; the second group covers what Stage 2 adds.
 */
public final class GameTests {
    private static int passed;
    private static int failed;

    private GameTests() {
    }

    public static void main(String[] args) throws IOException {
        // Stage 1 rules that must still hold
        menuMatchesTheBrief();
        orderMovesThroughLegalStatesOnly();
        regularCustomerLosesEightPerWaitingTurnAndNothingOnceServed();
        customerTurnGuardsDelayEatingAndPayment();
        regularCustomerChoosesFoodByIdRule();
        tableHoldsOneCustomer();
        paymentRequiresReadyToPayAndFreesTable();
        abandonmentCancelsOrderFreesTableAndLeavesQueue();
        // Stage 2: VIP and Critic
        mostExpensiveItemIsPasta();
        lossPerWaitingTurnDependsOnTheCustomerType();
        vipPaysNinetyPercentAndOthersFullPrice();
        waiterLocksTheBillWhenTheOrderIsTaken();
        criticAlwaysOrdersTheMostExpensiveDish();
        criticIsUpsetWhenServedMoreThanTwoTurnsAfterOrdering();
        scheduleHasSixArrivalsIncludingVipAndCritic();
        controllerDispatchesThroughCustomersNotTypeChecks();
        // Full recorded runs
        victoryRunIsReproducible();
        defeatRunIsReproducible();
        invalidChoicesDoNotUseUpTheTask();
        System.out.println(passed + " checks passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---- Stage 1 rules, re-checked --------------------------------------------------------

    private static void menuMatchesTheBrief() {
        Menu menu = new Restaurant().getMenu();
        check("menu has three items", menu.size() == 3);
        check("Salad $9.00, 1 unit", item(menu, "Salad", 900, 1));
        check("Burger $15.00, 1 unit", item(menu, "Burger", 1500, 1));
        check("Pasta $18.00, 2 units", item(menu, "Pasta", 1800, 2));
    }

    private static void orderMovesThroughLegalStatesOnly() {
        Order order = new Order(new MenuItem("Pasta", 1800, 2), 1800, 1);
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

        Order burger = new Order(new MenuItem("Burger", 1500, 1), 1500, 1);
        burger.advancePreparation();
        check("a 1-unit dish goes PLACED to READY", burger.isReady());
    }

    private static void regularCustomerLosesEightPerWaitingTurnAndNothingOnceServed() {
        Customer waiting = new RegularCustomer(1);
        waiting.reduceSatisfaction();
        check("waiting customer loses 8", waiting.getSatisfaction() == 92);
        waiting.markSeated();
        waiting.reduceSatisfaction();
        check("seated customer loses 8", waiting.getSatisfaction() == 84);
        waiting.assignOrder(new Order(new MenuItem("Burger", 1500, 1), 1500, 2));
        waiting.reduceSatisfaction();
        check("customer with a placed order still loses 8", waiting.getSatisfaction() == 76);
        waiting.markServed(5);
        waiting.reduceSatisfaction();
        check("served customer loses nothing", waiting.getSatisfaction() == 76);

        Customer patient = new RegularCustomer(2);
        for (int turn = 1; turn <= 12; turn++) {
            patient.reduceSatisfaction();
        }
        check("after 12 waiting turns the customer has 4 left and stays",
                patient.getSatisfaction() == 4 && !patient.shouldAbandon());
        patient.reduceSatisfaction();
        check("satisfaction bottoms out at 0, then the customer abandons",
                patient.getSatisfaction() == 0 && patient.shouldAbandon());
    }

    private static void customerTurnGuardsDelayEatingAndPayment() {
        Customer customer = new RegularCustomer(1);
        customer.markSeated();
        customer.assignOrder(new Order(new MenuItem("Burger", 1500, 1), 1500, 2));
        customer.markServed(4);
        check("cannot finish eating on the serving turn", !customer.canFinishEating(4));
        check("can finish eating a turn later", customer.canFinishEating(5));
        customer.markReadyToPay(5);
        check("cannot pay on the READY_TO_PAY turn", !customer.canPay(5));
        check("can pay a turn later", customer.canPay(6));
        expectRejected("marking READY_TO_PAY twice is rejected", () -> customer.markReadyToPay(6));
    }

    private static void regularCustomerChoosesFoodByIdRule() {
        Menu menu = new Restaurant().getMenu();
        check("customer 1 picks Burger", new RegularCustomer(1).chooseMenuItem(menu).getName().equals("Burger"));
        check("customer 2 picks Pasta", new RegularCustomer(2).chooseMenuItem(menu).getName().equals("Pasta"));
        check("customer 3 picks Salad", new RegularCustomer(3).chooseMenuItem(menu).getName().equals("Salad"));
    }

    private static void tableHoldsOneCustomer() {
        Table table = new Table(1);
        Customer first = new RegularCustomer(1);
        table.seat(first);
        check("seated table is not available", !table.isAvailable() && table.getOccupied() == first);
        expectRejected("a second customer cannot take an occupied table", () -> table.seat(new RegularCustomer(2)));
        table.release();
        check("released table is available", table.isAvailable());
    }

    private static void paymentRequiresReadyToPayAndFreesTable() {
        Restaurant restaurant = new Restaurant();
        Customer customer = new RegularCustomer(1);
        restaurant.registerArrival(customer);
        Table table = restaurant.findAvailableTable();
        restaurant.getWaiter().seatCustomer(customer, table);
        restaurant.getWaitingQueue().remove(customer);
        expectRejected("the Cashier cannot charge a seated customer",
                () -> restaurant.getCashier().processPayment(customer, restaurant));
        restaurant.getWaiter().takeOrder(customer, restaurant.getMenu(), 2);
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
        Customer seated = new RegularCustomer(1);
        Customer queued = new RegularCustomer(2);
        restaurant.registerArrival(seated);
        restaurant.registerArrival(queued);
        Table table = restaurant.findAvailableTable();
        restaurant.getWaiter().seatCustomer(seated, table);
        restaurant.getWaitingQueue().remove(seated);
        restaurant.getWaiter().takeOrder(seated, restaurant.getMenu(), 2);
        restaurant.handleAbandonment(seated);
        check("abandoned customer is ABANDONED", seated.getStatus() == CustomerStatus.ABANDONED);
        check("abandoned customer's order is CANCELLED", seated.getOrder().getStatus() == OrderStatus.CANCELLED);
        check("abandoned customer's table is free", table.isAvailable());
        restaurant.handleAbandonment(queued);
        check("a queued customer who leaves is removed from the queue", restaurant.getWaitingQueue().isEmpty());
        check("two unhappy departures are counted", restaurant.getUnhappyDepartureCount() == 2);
        check("abandoned customers add no revenue", restaurant.getRevenueCents() == 0);
    }

    // ---- Stage 2: VIP and Critic ----------------------------------------------------------

    private static void mostExpensiveItemIsPasta() {
        check("the most expensive menu item is Pasta", new Restaurant().getMenu().getMostExpensive().getName().equals("Pasta"));
    }

    private static void lossPerWaitingTurnDependsOnTheCustomerType() {
        // Each is used only through the Customer type, as the controller does.
        Customer[] customers = {new RegularCustomer(1), new VIPCustomer(2), new CriticCustomer(3)};
        int[] expectedSatisfaction = {92, 95, 88};
        for (int i = 0; i < customers.length; i++) {
            customers[i].reduceSatisfaction();
            check(customers[i].getTypeLabel() + " loses " + (100 - expectedSatisfaction[i]) + " per waiting turn",
                    customers[i].getSatisfaction() == expectedSatisfaction[i]);
        }
        Customer critic = new CriticCustomer(3);
        for (int turn = 1; turn <= 8; turn++) {
            critic.reduceSatisfaction();
        }
        check("a Critic still has 4 after 8 waiting turns", critic.getSatisfaction() == 4 && !critic.shouldAbandon());
        critic.reduceSatisfaction();
        check("a Critic reaches 0 on the 9th waiting turn and abandons", critic.getSatisfaction() == 0 && critic.shouldAbandon());
    }

    private static void vipPaysNinetyPercentAndOthersFullPrice() {
        check("VIP Burger: $15.00 becomes $13.50", new VIPCustomer(1).calculateBillCents(1500) == 1350);
        check("VIP Salad: $9.00 becomes $8.10", new VIPCustomer(1).calculateBillCents(900) == 810);
        check("VIP Pasta: $18.00 becomes $16.20", new VIPCustomer(1).calculateBillCents(1800) == 1620);
        check("Regular pays full price", new RegularCustomer(1).calculateBillCents(1500) == 1500);
        check("Critic pays full price", new CriticCustomer(1).calculateBillCents(1800) == 1800);
    }

    private static void waiterLocksTheBillWhenTheOrderIsTaken() {
        Restaurant restaurant = new Restaurant();
        Customer vip = new VIPCustomer(1);
        restaurant.registerArrival(vip);
        restaurant.getWaiter().seatCustomer(vip, restaurant.findAvailableTable());
        restaurant.getWaitingQueue().remove(vip);
        Order order = restaurant.getWaiter().takeOrder(vip, restaurant.getMenu(), 2);
        check("the VIP orders a Burger", order.getItem().getName().equals("Burger"));
        check("the order's bill is locked at $13.50", order.getBillCents() == 1350);
        check("the order remembers the turn it was taken", order.getTurnTaken() == 2);
        restaurant.getChef().prepare(order);
        restaurant.getWaiter().serveOrder(vip, 3);
        vip.markReadyToPay(4);
        restaurant.getCashier().processPayment(vip, restaurant);
        check("the Cashier collects the locked $13.50", restaurant.getRevenueCents() == 1350);
    }

    private static void criticAlwaysOrdersTheMostExpensiveDish() {
        Menu menu = new Restaurant().getMenu();
        check("Regular 3 picks Salad by the id rule", new RegularCustomer(3).chooseMenuItem(menu).getName().equals("Salad"));
        check("Critic 3 picks Pasta whatever the id", new CriticCustomer(3).chooseMenuItem(menu).getName().equals("Pasta"));
        check("Critic 1 picks Pasta whatever the id", new CriticCustomer(1).chooseMenuItem(menu).getName().equals("Pasta"));
    }

    private static void criticIsUpsetWhenServedMoreThanTwoTurnsAfterOrdering() {
        check("Critic served 2 turns after ordering: no extra loss", satisfactionAfterServiceDelay(new CriticCustomer(1), 2) == 100);
        check("Critic served 3 turns after ordering: -10", satisfactionAfterServiceDelay(new CriticCustomer(1), 3) == 90);
        check("VIP served 6 turns after ordering: nothing extra", satisfactionAfterServiceDelay(new VIPCustomer(1), 6) == 100);
        check("Regular served 6 turns after ordering: nothing extra", satisfactionAfterServiceDelay(new RegularCustomer(1), 6) == 100);
    }

    /** Orders on turn 2 a 1-unit dish that is READY at once, serves it after the given delay. */
    private static int satisfactionAfterServiceDelay(Customer customer, int delayTurns) {
        Restaurant restaurant = new Restaurant();
        restaurant.registerArrival(customer);
        restaurant.getWaiter().seatCustomer(customer, restaurant.findAvailableTable());
        restaurant.getWaitingQueue().remove(customer);
        Order order = new Order(new MenuItem("Burger", 1500, 1), 1500, 2);
        customer.assignOrder(order);
        restaurant.getChef().prepare(order);
        restaurant.getWaiter().serveOrder(customer, 2 + delayTurns);
        return customer.getSatisfaction();
    }

    private static void scheduleHasSixArrivalsIncludingVipAndCritic() {
        ArrivalSchedule schedule = new ArrivalSchedule();
        int arrivals = 0;
        boolean vip = false;
        boolean critic = false;
        for (int turn = 1; turn <= 18; turn++) {
            boolean expected = turn % 3 == 1 && turn <= 16;
            check("arrival on turn " + turn + " is " + expected, schedule.isArrivalTurn(turn) == expected);
            if (schedule.isArrivalTurn(turn)) {
                Customer arrival = schedule.createArrival(turn);
                arrivals++;
                check("customers are numbered in arrival order", arrival.getId() == arrivals);
                vip |= arrival.getTypeLabel().equals("VIP");
                critic |= arrival.getTypeLabel().equals("Critic");
            }
        }
        check("six customers arrive", arrivals == 6);
        check("the base run includes a VIP and a Critic", vip && critic);
        expectRejected("no customer can be created on a non-arrival turn", () -> schedule.createArrival(2));
    }

    private static void controllerDispatchesThroughCustomersNotTypeChecks() throws IOException {
        String controller = Files.readString(Path.of("src/restaurantrush/control/GameController.java"));
        check("GameController contains no instanceof", !controller.contains("instanceof"));
        check("GameController never branches on the customer type", !controller.contains("Critic")
                && !controller.contains("VIP") && !controller.contains("Regular"));
    }

    // ---- full-game checks: replay the recorded inputs in runs/ ---------------------------

    private static void victoryRunIsReproducible() throws IOException {
        String transcript = play("runs/victory.txt");
        check("victory run ends in VICTORY", transcript.contains("\nVICTORY\n"));
        check("victory run: 5 paid", transcript.contains("Paid customers: 5 (need 4)"));
        check("victory run: $76.50", transcript.contains("Revenue: $76.50 (need $45.00)"));
        check("victory run: average 69.6", transcript.contains("Average satisfaction of paid customers: 69.6"));
        check("the game ran all 18 turns", transcript.contains("Turns used: 18"));
        check("exactly six customers arrive in the base setting", transcript.contains("  #6 ") && !transcript.contains("  #7 "));
        check("the VIP pays the discounted $13.50", transcript.contains("Cashier collects $13.50 from Customer #1 (VIP)"));
        check("the Critic orders Pasta", transcript.contains("Customer #4 (Critic). They choose Pasta"));
        check("the Critic, served 3 turns after ordering, pays with satisfaction 30",
                transcript.contains("Cashier collects $18.00 from Customer #4 (Critic) (satisfaction 30)"));
    }

    private static void defeatRunIsReproducible() throws IOException {
        String transcript = play("runs/defeat.txt");
        check("defeat run ends in DEFEAT", transcript.contains("\nDEFEAT\n"));
        check("defeat run: only the VIP paid", transcript.contains("Paid customers: 1 (need 4)")
                && transcript.contains("Revenue: $13.50 (need"));
        check("defeat run: average 90.0", transcript.contains("Average satisfaction of paid customers: 90.0 (need"));
        check("defeat run: a Regular and the Critic walked out", transcript.contains("Unhappy departures: 2")
                && transcript.contains("Customer #2 (Regular) runs out of patience")
                && transcript.contains("Customer #4 (Critic) runs out of patience"));
    }

    private static void invalidChoicesDoNotUseUpTheTask() throws IOException {
        String transcript = play("runs/victory.txt");
        check("a seat request with no free table is explained", transcript.contains("Both tables are occupied"));
        check("a rejected choice is followed by another prompt in the same turn",
                transcript.contains("Choose again.\nWaiter  [SEAT <#> | TAKE_ORDER <#> | SERVE <#> | WAIT]: "));
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

    private static boolean item(Menu menu, String name, int priceCents, int units) {
        for (MenuItem candidate : menu.getItems()) {
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
