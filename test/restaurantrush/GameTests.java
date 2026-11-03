package restaurantrush;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import restaurantrush.control.ArrivalSchedule;
import restaurantrush.entity.ComboMeal;
import restaurantrush.entity.CriticCustomer;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.HappyHour;
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
 * Exits with status 1 if any check fails. The first two groups re-check the Stage 1 and Stage 2
 * rules that must still hold; the third covers what Stage 3 adds.
 */
public final class GameTests {
    private static final MenuItem SALAD = new MenuItem("Salad", 900, 1);
    private static final MenuItem BURGER = new MenuItem("Burger", 1500, 1);
    private static final MenuItem PASTA = new MenuItem("Pasta", 1800, 2);

    private static int passed;
    private static int failed;

    private GameTests() {
    }

    public static void main(String[] args) throws IOException {
        // Stage 1 rules that must still hold
        orderMovesThroughLegalStatesOnly();
        regularCustomerLosesEightPerWaitingTurnAndNothingOnceServed();
        customerTurnGuardsDelayEatingAndPayment();
        regularCustomerChoosesFoodByIdRule();
        tableHoldsOneCustomer();
        paymentRequiresReadyToPayAndFreesTable();
        abandonmentCancelsOrderFreesTableAndLeavesQueue();
        // Stage 2 rules that must still hold
        lossPerWaitingTurnDependsOnTheCustomerType();
        vipPaysNinetyPercentAndOthersFullPrice();
        waiterLocksTheBillWhenTheOrderIsTaken();
        criticAlwaysOrdersTheMostExpensiveDish();
        criticIsUpsetWhenServedMoreThanTwoTurnsAfterOrdering();
        scheduleHasSixArrivalsIncludingVipAndCritic();
        controllerDispatchesThroughCustomersNotTypeChecks();
        // Stage 3: ComboMeal
        comboPriceAndUnitsMatchTheReferenceTable();
        menuOffersTwoRealCombosBesideTheThreeDishes();
        customersChooseCombosByTheirOwnRules();
        // Stage 3: Happy Hour
        happyHourLastsTwoTurnsAndWorksOncePerGame();
        happyHourDiscountMatchesTheReferenceTable();
        happyHourNeverChangesAnOrderThatIsAlreadyPlaced();
        happyHourLetsWaitingCustomersRecoverSatisfaction();
        // Stage 3: Host
        hostSeatsACustomerAndRejectsBadRequests();
        // Stage 3 clean-up: state protection and who owns which rule
        waitingQueueIsReadOnlyAndSeatingIsAtomic();
        restaurantKnowsWhatEachStaffMemberCanDo();
        restaurantKnowsWhenTheObjectiveIsMet();
        screenAndControllerDoNotDependOnEachOther();
        // Full recorded runs
        victoryRunIsReproducible();
        defeatRunIsReproducible();
        invalidChoicesDoNotUseUpTheTask();
        happyHourIsOfferedAgainAfterBeingDeclined();
        System.out.println(passed + " checks passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---- Stage 1 rules, re-checked --------------------------------------------------------

    private static void orderMovesThroughLegalStatesOnly() {
        Order order = new Order(PASTA, 1800, 1);
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

        Order burger = new Order(BURGER, 1500, 1);
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
        waiting.assignOrder(new Order(BURGER, 1500, 2));
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
        customer.assignOrder(new Order(BURGER, 1500, 2));
        customer.markServed(4);
        check("cannot finish eating on the serving turn", !customer.canFinishEating(4));
        check("can finish eating a turn later", customer.canFinishEating(5));
        customer.markReadyToPay(5);
        check("cannot pay on the READY_TO_PAY turn", !customer.canPay(5));
        check("can pay a turn later", customer.canPay(6));
        expectRejected("marking READY_TO_PAY twice is rejected", () -> customer.markReadyToPay(6));
    }

    private static void regularCustomerChoosesFoodByIdRule() {
        Menu threeDishes = baseMenu();
        check("customer 1 picks Burger", new RegularCustomer(1).chooseMenuItem(threeDishes).getName().equals("Burger"));
        check("customer 2 picks Pasta", new RegularCustomer(2).chooseMenuItem(threeDishes).getName().equals("Pasta"));
        check("customer 3 picks Salad", new RegularCustomer(3).chooseMenuItem(threeDishes).getName().equals("Salad"));
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
        restaurant.seatFromQueue(customer, table, restaurant.getWaiter()::seatCustomer);
        expectRejected("the Cashier cannot charge a seated customer",
                () -> restaurant.getCashier().processPayment(customer, restaurant));
        restaurant.getWaiter().takeOrder(customer, restaurant.getMenu(), restaurant.getHappyHour(), 2);
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
        restaurant.seatFromQueue(seated, table, restaurant.getWaiter()::seatCustomer);
        restaurant.getWaiter().takeOrder(seated, restaurant.getMenu(), restaurant.getHappyHour(), 2);
        restaurant.handleAbandonment(seated);
        check("abandoned customer is ABANDONED", seated.getStatus() == CustomerStatus.ABANDONED);
        check("abandoned customer's order is CANCELLED", seated.getOrder().getStatus() == OrderStatus.CANCELLED);
        check("abandoned customer's table is free", table.isAvailable());
        restaurant.handleAbandonment(queued);
        check("a queued customer who leaves is removed from the queue", restaurant.getWaitingQueue().isEmpty());
        check("two unhappy departures are counted", restaurant.getUnhappyDepartureCount() == 2);
        check("abandoned customers add no revenue", restaurant.getRevenueCents() == 0);
    }

    // ---- Stage 2 rules, re-checked --------------------------------------------------------

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
        restaurant.seatFromQueue(vip, restaurant.findAvailableTable(), restaurant.getWaiter()::seatCustomer);
        Order order = restaurant.getWaiter().takeOrder(vip, restaurant.getMenu(), restaurant.getHappyHour(), 2);
        check("the VIP orders a Burger", order.getItem().getName().equals("Burger"));
        check("without Happy Hour the bill is locked at $13.50", order.getBillCents() == 1350);
        check("the order remembers the turn it was taken", order.getTurnTaken() == 2);
        restaurant.getChef().prepare(order);
        restaurant.getWaiter().serveOrder(vip, 3);
        vip.markReadyToPay(4);
        restaurant.getCashier().processPayment(vip, restaurant);
        check("the Cashier collects the locked $13.50", restaurant.getRevenueCents() == 1350);
    }

    private static void criticAlwaysOrdersTheMostExpensiveDish() {
        Menu threeDishes = baseMenu();
        check("on the three-dish menu the most expensive item is Pasta", threeDishes.getMostExpensive().getName().equals("Pasta"));
        check("Regular 3 picks Salad by the id rule", new RegularCustomer(3).chooseMenuItem(threeDishes).getName().equals("Salad"));
        check("Critic 3 picks Pasta whatever the id", new CriticCustomer(3).chooseMenuItem(threeDishes).getName().equals("Pasta"));
        check("Critic 1 picks Pasta whatever the id", new CriticCustomer(1).chooseMenuItem(threeDishes).getName().equals("Pasta"));
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
        restaurant.seatFromQueue(customer, restaurant.findAvailableTable(), restaurant.getWaiter()::seatCustomer);
        Order order = new Order(BURGER, 1500, 2);
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
        for (String name : List.of("GameController", "StaffActions", "ManagerCommand")) {
            String source = Files.readString(Path.of("src/restaurantrush/control/" + name + ".java"));
            check(name + " contains no instanceof", !source.contains("instanceof"));
            check(name + " never branches on the customer type", !source.contains("Critic")
                    && !source.contains("VIP") && !source.contains("Regular"));
        }
    }

    // ---- Stage 3: ComboMeal ---------------------------------------------------------------

    private static void comboPriceAndUnitsMatchTheReferenceTable() {
        ComboMeal saladBurger = new ComboMeal(SALAD, BURGER);
        ComboMeal saladPasta = new ComboMeal(SALAD, PASTA);
        ComboMeal burgerPasta = new ComboMeal(BURGER, PASTA);
        check("Salad + Burger costs $21.60", saladBurger.getPriceCents() == 2160);
        check("Salad + Burger takes 2 units", saladBurger.getPreparationUnits() == 2);
        check("Salad + Pasta costs $24.30", saladPasta.getPriceCents() == 2430);
        check("Salad + Pasta takes 3 units", saladPasta.getPreparationUnits() == 3);
        check("Burger + Pasta costs $29.70", burgerPasta.getPriceCents() == 2970);
        check("Burger + Pasta takes 3 units", burgerPasta.getPreparationUnits() == 3);
        check("a combo is named after its components", saladPasta.getName().equals("Salad + Pasta"));
        List<MenuItem> components = saladPasta.getComponents();
        check("a combo is made of two existing menu items", components.size() == 2
                && components.get(0) == SALAD && components.get(1) == PASTA);
        MenuItem asMenuItem = saladPasta;
        Order order = new Order(asMenuItem, asMenuItem.getPriceCents(), 1);
        order.advancePreparation();
        order.advancePreparation();
        check("a 3-unit combo is not READY after 2 units", !order.isReady());
        order.advancePreparation();
        check("a 3-unit combo is READY after 3 units", order.isReady());
    }

    private static void menuOffersTwoRealCombosBesideTheThreeDishes() {
        Menu menu = new Restaurant().getMenu();
        check("the menu has five entries", menu.size() == 5);
        check("the first three entries are the plain dishes", menu.getItem(0).getName().equals("Salad")
                && menu.getItem(1).getName().equals("Burger") && menu.getItem(2).getName().equals("Pasta"));
        check("entry 3 is the Salad + Burger combo", menu.getItem(3) instanceof ComboMeal
                && menu.getItem(3).getPriceCents() == 2160);
        check("entry 4 is the Salad + Pasta combo", menu.getItem(4) instanceof ComboMeal
                && menu.getItem(4).getPriceCents() == 2430);
        check("the combo components are the menu's own dishes",
                ((ComboMeal) menu.getItem(4)).getComponents().get(1) == menu.getItem(2));
    }

    private static void customersChooseCombosByTheirOwnRules() {
        Menu menu = new Restaurant().getMenu();
        check("Regular 3 takes entry 3 (3 mod 5): Salad + Burger",
                new RegularCustomer(3).chooseMenuItem(menu).getName().equals("Salad + Burger"));
        check("VIP 4 takes entry 4 (4 mod 5): Salad + Pasta",
                new VIPCustomer(4).chooseMenuItem(menu).getName().equals("Salad + Pasta"));
        check("Regular 1 still orders a Burger", new RegularCustomer(1).chooseMenuItem(menu).getName().equals("Burger"));
        check("the Critic orders the most expensive entry, Salad + Pasta",
                new CriticCustomer(1).chooseMenuItem(menu).getName().equals("Salad + Pasta"));
    }

    // ---- Stage 3: Happy Hour ---------------------------------------------------------------

    private static void happyHourLastsTwoTurnsAndWorksOncePerGame() {
        HappyHour happyHour = new HappyHour();
        check("Happy Hour starts available and inactive", happyHour.isAvailable() && !happyHour.isActive()
                && happyHour.getRemainingTurns() == 0);
        check("it can be switched on", happyHour.activate());
        check("it is active with 2 turns remaining", happyHour.isActive() && happyHour.getRemainingTurns() == 2);
        check("it is no longer available once used", !happyHour.isAvailable());
        happyHour.endTurn();
        check("after the first turn it is still active with 1 remaining", happyHour.isActive()
                && happyHour.getRemainingTurns() == 1);
        happyHour.endTurn();
        check("after the second turn it is over", !happyHour.isActive() && happyHour.getRemainingTurns() == 0);
        check("it cannot be switched on a second time", !happyHour.activate());
        check("a rejected second activation does not restart it", !happyHour.isActive());
        happyHour.endTurn();
        check("ending more turns keeps it at 0", happyHour.getRemainingTurns() == 0);
    }

    private static void happyHourDiscountMatchesTheReferenceTable() {
        Restaurant vipRestaurant = restaurantWithHappyHourOn();
        Customer vip = new VIPCustomer(1);
        Order vipOrder = orderFor(vipRestaurant, vip);
        check("VIP Burger during Happy Hour: $15 x 0.9 x 0.8 = $10.80", vipOrder.getBillCents() == 1080);

        Restaurant regularRestaurant = restaurantWithHappyHourOn();
        Order regularOrder = orderFor(regularRestaurant, new RegularCustomer(2));
        check("Regular Pasta during Happy Hour: $14.40", regularOrder.getBillCents() == 1440);

        Restaurant comboRestaurant = restaurantWithHappyHourOn();
        Order comboOrder = orderFor(comboRestaurant, new CriticCustomer(4));
        check("Critic's Salad + Pasta combo during Happy Hour: $24.30 less 20% = $19.44",
                comboOrder.getItem().getName().equals("Salad + Pasta") && comboOrder.getBillCents() == 1944);

        HappyHour inactive = new HappyHour();
        check("with Happy Hour off the bill is unchanged", inactive.applyDiscount(1350) == 1350);
    }

    private static void happyHourNeverChangesAnOrderThatIsAlreadyPlaced() {
        Restaurant restaurant = new Restaurant();
        Order before = orderFor(restaurant, new RegularCustomer(2));
        check("an order taken before Happy Hour is full price", before.getBillCents() == 1800);
        restaurant.getHappyHour().activate();
        check("switching Happy Hour on does not change it", before.getBillCents() == 1800);
        Order during = orderFor(restaurant, new RegularCustomer(5));
        check("an order taken during Happy Hour gets the discount", during.getBillCents() == 720);
        restaurant.getHappyHour().endTurn();
        restaurant.getHappyHour().endTurn();
        check("the discount is locked after Happy Hour ends", during.getBillCents() == 720);
        Order after = orderFor(restaurant, new RegularCustomer(7));
        check("an order taken after Happy Hour is over is full price", after.getBillCents() == 1800);
    }

    private static void happyHourLetsWaitingCustomersRecoverSatisfaction() {
        HappyHour happyHour = new HappyHour();
        check("no recovery while Happy Hour is off", happyHour.getRecoveryAmount() == 0);
        happyHour.activate();
        check("recovery is 15 per active turn", happyHour.getRecoveryAmount() == 15);

        Customer waiting = new RegularCustomer(1);
        waiting.reduceSatisfaction();
        waiting.reduceSatisfaction();
        waiting.recoverSatisfaction(happyHour.getRecoveryAmount());
        check("a waiting customer recovers 15 (84 to 99)", waiting.getSatisfaction() == 99);
        waiting.recoverSatisfaction(15);
        check("satisfaction never goes above 100", waiting.getSatisfaction() == 100);

        Customer served = new RegularCustomer(2);
        served.markSeated();
        served.reduceSatisfaction();
        served.assignOrder(new Order(BURGER, 1500, 1));
        served.markServed(2);
        served.recoverSatisfaction(15);
        check("a served customer does not recover", served.getSatisfaction() == 92);

        Customer gone = new RegularCustomer(3);
        for (int turn = 1; turn <= 13; turn++) {
            gone.reduceSatisfaction();
        }
        gone.markAbandoned();
        gone.recoverSatisfaction(15);
        check("a customer who has left does not recover", gone.getSatisfaction() == 0);
    }

    // ---- Stage 3: Host ---------------------------------------------------------------------

    private static void hostSeatsACustomerAndRejectsBadRequests() {
        Restaurant restaurant = new Restaurant();
        Customer first = new RegularCustomer(1);
        Customer second = new RegularCustomer(2);
        restaurant.registerArrival(first);
        restaurant.registerArrival(second);
        Table table = restaurant.findAvailableTable();
        restaurant.getHost().seatCustomer(first, table);
        check("the Host seats a queued customer", first.getStatus() == CustomerStatus.SEATED
                && table.getOccupied() == first);
        expectRejected("the Host cannot seat at an occupied table", () -> restaurant.getHost().seatCustomer(second, table));
        check("a rejected seating leaves the customer waiting", second.getStatus() == CustomerStatus.WAITING);
        Table other = restaurant.findAvailableTable();
        expectRejected("the Host cannot seat a customer who is already seated",
                () -> restaurant.getHost().seatCustomer(first, other));
        check("a rejected seating leaves the other table free", other.isAvailable());
        restaurant.getWaiter().seatCustomer(second, other);
        check("the Waiter can still seat customers", second.getStatus() == CustomerStatus.SEATED);
    }

    // ---- full-game checks: replay the recorded inputs in runs/ ---------------------------

    private static void victoryRunIsReproducible() throws IOException {
        String transcript = playFile("runs/victory.txt");
        check("victory run ends in VICTORY", transcript.contains("\nVICTORY\n"));
        check("victory run: 5 paid", transcript.contains("Paid customers: 5 (need 4)"));
        check("victory run: $83.70", transcript.contains("Revenue: $83.70 (need $45.00)"));
        check("victory run: average 81.2", transcript.contains("Average satisfaction of paid customers: 81.2"));
        check("the game ran all 18 turns", transcript.contains("Turns used: 18"));
        check("exactly six customers arrive in the base setting", transcript.contains("  #6 ") && !transcript.contains("  #7 "));
        check("Happy Hour is offered only until it is used", count(transcript, "Happy Hour [y") == 1);
        check("the status shows Happy Hour's remaining duration",
                transcript.contains("Happy Hour: ACTIVE, 1 turn(s) left including this one"));
        check("the status shows Happy Hour as used afterwards", transcript.contains("Happy Hour: used"));
        check("the VIP's Burger is locked at $10.80 during Happy Hour",
                transcript.contains("They choose Burger (list price $15.00, 1 preparation unit(s)). Bill locked at $10.80."));
        check("the VIP pays $10.80", transcript.contains("Cashier collects $10.80 from Customer #1 (VIP)"));
        check("a Regular orders the Salad + Burger combo at $21.60, 2 units",
                transcript.contains("Customer #3 (Regular). They choose Salad + Burger (list price $21.60, 2 preparation unit(s))"));
        check("the Critic orders the Salad + Pasta combo at $24.30, 3 units",
                transcript.contains("Customer #4 (Critic). They choose Salad + Pasta (list price $24.30, 3 preparation unit(s))"));
        check("the Critic, served 3 turns after ordering, pays with satisfaction 54",
                transcript.contains("Cashier collects $24.30 from Customer #4 (Critic) (satisfaction 54)"));
        check("the Host seats a customer and the Waiter takes the order in the same turn", transcript.contains(
                "* Host seats Customer #1 (VIP) at Table 1.\n"
                        + "Waiter  [SEAT <#> | TAKE_ORDER <#> | SERVE <#> | WAIT]: TAKE_ORDER 1\n"
                        + "  * Waiter takes the order of Customer #1 (VIP)."));
    }

    private static void defeatRunIsReproducible() throws IOException {
        String transcript = playFile("runs/defeat.txt");
        check("defeat run ends in DEFEAT", transcript.contains("\nDEFEAT\n"));
        check("defeat run: only the VIP paid", transcript.contains("Paid customers: 1 (need 4)")
                && transcript.contains("Revenue: $10.80 (need"));
        check("defeat run: average 100.0", transcript.contains("Average satisfaction of paid customers: 100.0 (need"));
        check("defeat run: a Regular and the Critic walked out", transcript.contains("Unhappy departures: 2")
                && transcript.contains("Customer #2 (Regular) runs out of patience")
                && transcript.contains("Customer #4 (Critic) runs out of patience"));
    }

    private static void invalidChoicesDoNotUseUpTheTask() throws IOException {
        String transcript = playFile("runs/victory.txt");
        check("a Host request for a customer who has not arrived is explained", transcript.contains("There is no customer #5"));
        check("a Waiter request for a customer who has not arrived is explained", transcript.contains("There is no customer #3. Choose again."));
        check("a rejected Host choice is followed by another Host prompt in the same turn",
                transcript.contains("Choose again.\nHost    [SEAT <#> | WAIT]: "));
        check("a rejected Waiter choice is followed by another Waiter prompt in the same turn",
                transcript.contains("Choose again.\nWaiter  [SEAT <#> | TAKE_ORDER <#> | SERVE <#> | WAIT]: "));
    }

    private static void happyHourIsOfferedAgainAfterBeingDeclined() {
        // Turn 1: decline Happy Hour, then WAIT for the Host, Waiter and Chef; turn 2: nonsense, then accept.
        String transcript = playText("n\nWAIT\nWAIT\nWAIT\nmaybe\ny\n");
        check("Happy Hour is offered again on the next turn", count(transcript, "Happy Hour [y") == 3);
        check("an answer other than y or n is rejected", transcript.contains("'maybe' is not y or n"));
        check("accepting it on turn 2 switches it on for that turn and the next",
                transcript.contains("Happy Hour is ON: orders taken now get a discount and waiting customers recover "
                        + "satisfaction. Turns left: 2."));
    }

    // ---- Stage 3 clean-up ------------------------------------------------------------------

    private static void waitingQueueIsReadOnlyAndSeatingIsAtomic() {
        Restaurant restaurant = new Restaurant();
        Customer first = new RegularCustomer(1);
        Customer second = new RegularCustomer(2);
        Customer third = new RegularCustomer(3);
        for (Customer customer : List.of(first, second, third)) {
            restaurant.registerArrival(customer);
        }
        try {
            restaurant.getWaitingQueue().remove(first);
            check("the waiting queue cannot be changed from outside", false);
        } catch (UnsupportedOperationException expected) {
            check("the waiting queue cannot be changed from outside", true);
        }
        check("the queue keeps arrival order", restaurant.getWaitingQueue().equals(List.of(first, second, third)));

        Table table = restaurant.findAvailableTable();
        restaurant.seatFromQueue(second, table, restaurant.getHost()::seatCustomer);
        check("a seated customer leaves the queue and the others keep their places",
                restaurant.getWaitingQueue().equals(List.of(first, third)) && second.getStatus() == CustomerStatus.SEATED);

        expectRejected("seating at an occupied table is rejected", () ->
                restaurant.seatFromQueue(first, table, restaurant.getWaiter()::seatCustomer));
        check("a rejected seating leaves the customer in the queue",
                restaurant.getWaitingQueue().contains(first) && first.getStatus() == CustomerStatus.WAITING);
        expectRejected("a customer who is not in the queue cannot be seated from it", () ->
                restaurant.seatFromQueue(second, restaurant.findAvailableTable(), restaurant.getWaiter()::seatCustomer));
        check("a rejected request for a seated customer does not seat them twice",
                second.getStatus() == CustomerStatus.SEATED && table.getOccupied() == second);
    }

    private static void restaurantKnowsWhatEachStaffMemberCanDo() {
        Restaurant restaurant = new Restaurant();
        Customer first = new RegularCustomer(1);
        Customer second = new RegularCustomer(2);
        Customer third = new RegularCustomer(3);
        for (Customer customer : List.of(first, second, third)) {
            restaurant.registerArrival(customer);
        }
        check("with a free table everyone in the queue can be seated", restaurant.getSeatableCustomers().size() == 3);
        check("nobody has ordered, so there is nothing to cook or serve",
                restaurant.getCustomersToCook().isEmpty() && restaurant.getCustomersToServe().isEmpty());

        restaurant.seatFromQueue(first, restaurant.findAvailableTable(), restaurant.getWaiter()::seatCustomer);
        restaurant.seatFromQueue(second, restaurant.findAvailableTable(), restaurant.getHost()::seatCustomer);
        check("with both tables occupied nobody can be seated", restaurant.getSeatableCustomers().isEmpty());
        check("the seated customers are waiting to order",
                restaurant.getCustomersWithStatus(CustomerStatus.SEATED).equals(List.of(first, second)));

        restaurant.getWaiter().takeOrder(first, restaurant.getMenu(), restaurant.getHappyHour(), 2);
        restaurant.getWaiter().takeOrder(second, restaurant.getMenu(), restaurant.getHappyHour(), 2);
        check("an order that is not finished is for the Chef, not the Waiter",
                restaurant.getCustomersToCook().equals(List.of(first, second)) && restaurant.getCustomersToServe().isEmpty());
        restaurant.getChef().prepare(first.getOrder());
        check("once READY it moves from the Chef list to the Waiter list",
                restaurant.getCustomersToServe().equals(List.of(first)) && restaurant.getCustomersToCook().equals(List.of(second)));
    }

    private static void restaurantKnowsWhenTheObjectiveIsMet() {
        Restaurant restaurant = new Restaurant();
        check("a restaurant that has served nobody has not met the objective", !restaurant.hasMetObjective());
        for (int id = 1; id <= Restaurant.REQUIRED_PAID_CUSTOMERS; id++) {
            Customer customer = new RegularCustomer(id);
            restaurant.registerArrival(customer);
            restaurant.seatFromQueue(customer, restaurant.findAvailableTable(), restaurant.getWaiter()::seatCustomer);
            customer.assignOrder(new Order(PASTA, 1800, 1));
            restaurant.getChef().prepare(customer.getOrder());
            restaurant.getChef().prepare(customer.getOrder());
            restaurant.getWaiter().serveOrder(customer, 2);
            customer.markReadyToPay(3);
            restaurant.getCashier().processPayment(customer, restaurant);
            if (id < Restaurant.REQUIRED_PAID_CUSTOMERS) {
                check("with " + id + " paid customers ($" + (18 * id) + ") the objective is not met", !restaurant.hasMetObjective());
            }
        }
        check("4 paid customers, $72.00 and average satisfaction 100 meet the objective", restaurant.hasMetObjective());
    }

    private static void screenAndControllerDoNotDependOnEachOther() throws IOException {
        String screen = Files.readString(Path.of("src/restaurantrush/boundary/RestaurantCLI.java"));
        check("the command-line screen does not use the controller", !screen.contains("GameController")
                && !screen.contains("restaurantrush.control"));
        check("the screen does not recompute which customers a staff member can act on",
                !screen.contains("CustomerStatus.ORDERED") && !screen.contains("isReady()")
                        && !screen.contains("findAvailableTable"));
        boolean globalState = false;
        for (java.lang.reflect.Field field : Order.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            globalState |= java.lang.reflect.Modifier.isStatic(modifiers) && !java.lang.reflect.Modifier.isFinal(modifiers);
        }
        check("an Order keeps no global mutable state (no static id counter)", !globalState);
    }

    // ---- tiny helpers --------------------------------------------------------------------

    private static Menu baseMenu() {
        return new Menu(List.of(SALAD, BURGER, PASTA));
    }

    private static Restaurant restaurantWithHappyHourOn() {
        Restaurant restaurant = new Restaurant();
        restaurant.getHappyHour().activate();
        return restaurant;
    }

    /** Seats the customer, has the Waiter take their order at turn 2, then frees the table again. */
    private static Order orderFor(Restaurant restaurant, Customer customer) {
        restaurant.registerArrival(customer);
        restaurant.seatFromQueue(customer, restaurant.findAvailableTable(), restaurant.getWaiter()::seatCustomer);
        Order order = restaurant.getWaiter().takeOrder(customer, restaurant.getMenu(), restaurant.getHappyHour(), 2);
        restaurant.releaseTableOf(customer);
        return order;
    }

    private static String playFile(String inputFile) throws IOException {
        try (FileInputStream choices = new FileInputStream(inputFile)) {
            return play(choices);
        }
    }

    private static String playText(String choices) {
        return play(new ByteArrayInputStream(choices.getBytes(StandardCharsets.UTF_8)));
    }

    private static String play(InputStream choices) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            Main.play(choices, out, true);
        }
        return captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static int count(String text, String part) {
        int found = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + 1)) {
            found++;
        }
        return found;
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
