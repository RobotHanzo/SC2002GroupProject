package restaurantrush.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import restaurantrush.boundary.RestaurantCLI;
import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.MenuItem;
import restaurantrush.entity.Money;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Table;

/**
 * Runs the game: 18 turns of six steps each (arrivals, manager choices, kitchen and serving,
 * eating and payment, waiting and abandonment, result). It owns the turn counter and decides
 * whether a choice is legal; the CLI only collects choices and displays state.
 */
public class GameController {
    public static final int REQUIRED_PAID_CUSTOMERS = 4;
    public static final int REQUIRED_REVENUE_CENTS = 4500;
    public static final int REQUIRED_AVERAGE_SATISFACTION = 60;

    private static final int MAX_TURNS = 18;

    private final Restaurant restaurant;
    private final ArrivalSchedule schedule;
    private int currentTurn;
    private final int maxTurns;
    private final RestaurantCLI cli;

    public GameController(RestaurantCLI cli) {
        this.cli = cli;
        this.restaurant = new Restaurant();
        this.schedule = new ArrivalSchedule();
        this.maxTurns = MAX_TURNS;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    /** Plays all turns (there is no early ending), then reports victory or defeat. */
    public void startGame() {
        while (currentTurn < maxTurns) {
            currentTurn++;
            executeTurn();
        }
        cli.displayResult(evaluateGameResult(), restaurant);
    }

    private void executeTurn() {
        processScheduledArrival();
        cli.displayTurnStatus(restaurant);
        processHappyHourActivation();
        applyHappyHourRecovery();
        coordinateStaffActions();
        coordinatePaymentProcessing();
        processEndOfTurnUpdates();
    }

    /** Step 1: the schedule decides who arrives at the start of turns 1, 4, 7, 10, 13 and 16. */
    private void processScheduledArrival() {
        if (schedule.isArrivalTurn(currentTurn)) {
            restaurant.registerArrival(schedule.createArrival(currentTurn));
        }
    }

    /** Start of turn: while Happy Hour is unused the manager may switch it on (once per game). */
    private void processHappyHourActivation() {
        HappyHour happyHour = restaurant.getHappyHour();
        if (happyHour.isAvailable() && cli.readHappyHourChoice() && happyHour.activate()) {
            cli.displayMessage("Happy Hour is ON: orders taken now get a discount and waiting customers recover "
                    + "satisfaction. Turns left: " + happyHour.getRemainingTurns() + ".");
        }
    }

    /** Start of an active Happy Hour turn: customers still waiting for a table or food recover satisfaction. */
    private void applyHappyHourRecovery() {
        HappyHour happyHour = restaurant.getHappyHour();
        if (!happyHour.isActive()) {
            return;
        }
        int amount = happyHour.getRecoveryAmount();
        List<String> changes = new ArrayList<>();
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.isAwaitingService()) {
                int before = customer.getSatisfaction();
                customer.recoverSatisfaction(amount);
                changes.add("#" + customer.getId() + " " + before + " -> " + customer.getSatisfaction());
            }
        }
        cli.displayMessage("Happy Hour (" + happyHour.getRemainingTurns() + " turn(s) left including this one): "
                + (changes.isEmpty() ? "nobody is waiting." : "waiting customers recover " + String.join(", ", changes) + "."));
    }

    /** Steps 2 and 3: the Waiter acts first, then the Chef. A rejected choice does not use up the task. */
    private void coordinateStaffActions() {
        chooseUntilAccepted(cli::readWaiterChoice, this::performWaiterTask);
        chooseUntilAccepted(cli::readChefChoice, this::performChefTask);
    }

    private void chooseUntilAccepted(Supplier<String> reader, Consumer<String> task) {
        while (true) {
            String choice = reader.get();
            try {
                task.accept(choice);
                return;
            } catch (IllegalArgumentException | IllegalStateException rejected) {
                cli.displayError(rejected.getMessage());
            }
        }
    }

    private void performWaiterTask(String choice) {
        Command command = Command.parse(choice);
        switch (command.verb) {
            case "WAIT" -> { }
            case "SEAT" -> seatQueuedCustomer(command, restaurant.getWaiter()::seatCustomer, "Waiter");
            case "TAKE_ORDER", "ORDER", "TAKE" -> takeOrder(command);
            case "SERVE" -> serve(command);
            default -> throw new IllegalArgumentException(
                    "Unknown Waiter command '" + command.verb + "'. Use SEAT, TAKE_ORDER, SERVE or WAIT.");
        }
    }

    private void performChefTask(String choice) {
        Command command = Command.parse(choice);
        switch (command.verb) {
            case "WAIT" -> { }
            case "COOK", "PREPARE", "ADVANCE" -> prepare(command);
            default -> throw new IllegalArgumentException(
                    "Unknown Chef command '" + command.verb + "'. Use COOK or WAIT.");
        }
    }

    private void seatQueuedCustomer(Command command, BiConsumer<Customer, Table> seater, String actor) {
        Customer customer = select(command, new ArrayList<>(restaurant.getWaitingQueue()),
                "nobody is in the waiting queue", "only a customer in the waiting queue can be seated");
        Table table = restaurant.findAvailableTable();
        if (table == null) {
            throw new IllegalStateException("Both tables are occupied; a table frees up when its customer pays or leaves");
        }
        seater.accept(customer, table);
        restaurant.getWaitingQueue().remove(customer);
        cli.displayMessage(actor + " seats " + describe(customer) + " at Table " + table.getTableId() + ".");
    }

    private void takeOrder(Command command) {
        Customer customer = select(command, customersWithStatus(CustomerStatus.SEATED),
                "no seated customer is waiting to order", "only a seated customer can order");
        Order order = restaurant.getWaiter().takeOrder(customer, restaurant.getMenu(), restaurant.getHappyHour(), currentTurn);
        MenuItem item = order.getItem();
        cli.displayMessage("Waiter takes the order of " + describe(customer) + ". They choose "
                + item.getName() + " (list price " + Money.format(item.getPriceCents()) + ", "
                + item.getPreparationUnits() + " preparation unit(s)). Bill locked at "
                + Money.format(order.getBillCents()) + ".");
    }

    private void serve(Command command) {
        Customer customer = select(command, customersWithStatus(CustomerStatus.ORDERED),
                "no customer has an order to serve", "only a customer who has ordered can be served");
        restaurant.getWaiter().serveOrder(customer, currentTurn);
        cli.displayMessage("Waiter serves the " + customer.getOrder().getItem().getName() + " to "
                + describe(customer) + ".");
    }

    private void prepare(Command command) {
        Customer customer = select(command, customersWithStatus(CustomerStatus.ORDERED),
                "no order has been taken yet", "only a customer who has ordered has food to prepare");
        Order order = customer.getOrder();
        restaurant.getChef().prepare(order);
        cli.displayMessage("Chef works on the " + order.getItem().getName() + " for " + describe(customer)
                + ": " + order.getCompletedUnits() + "/" + order.getItem().getPreparationUnits()
                + " units, order is " + order.getStatus() + ".");
    }

    /** Step 4: served customers finish eating, then the Cashier charges at most one READY_TO_PAY customer. */
    private void coordinatePaymentProcessing() {
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.canFinishEating(currentTurn)) {
                customer.markReadyToPay(currentTurn);
                cli.displayMessage(describe(customer) + " finishes eating and is ready to pay.");
            }
        }
        Customer payer = restaurant.findOldestEligiblePayer(currentTurn);
        if (payer != null) {
            int bill = payer.getOrder().getBillCents();
            restaurant.getCashier().processPayment(payer, restaurant);
            cli.displayMessage("Cashier collects " + Money.format(bill) + " from " + describe(payer)
                    + " (satisfaction " + payer.getSatisfaction() + "). The table is free again.");
        }
    }

    /** Step 5: customers who are still waiting lose satisfaction; at 0 they leave without paying. */
    private void processEndOfTurnUpdates() {
        for (Customer customer : restaurant.getCustomers()) {
            customer.reduceSatisfaction();
            if (customer.shouldAbandon()) {
                restaurant.handleAbandonment(customer);
                cli.displayMessage(describe(customer)
                        + " runs out of patience and leaves without paying. Any unfinished order is cancelled.");
            }
        }
        restaurant.getHappyHour().endTurn();
    }

    /** Step 6: victory needs all three targets. */
    private boolean evaluateGameResult() {
        return restaurant.getPaidCustomerCount() >= REQUIRED_PAID_CUSTOMERS
                && restaurant.getRevenueCents() >= REQUIRED_REVENUE_CENTS
                && restaurant.getAverageSatisfaction() >= REQUIRED_AVERAGE_SATISFACTION;
    }

    /** Resolves the customer a command refers to, or explains why it cannot. */
    private Customer select(Command command, List<Customer> candidates, String noCandidates, String ineligible) {
        if (command.customerId == null) {
            if (candidates.isEmpty()) {
                throw new IllegalStateException("Nothing to do: " + noCandidates);
            }
            if (candidates.size() > 1) {
                throw new IllegalArgumentException("Several customers qualify (" + idList(candidates)
                        + "); add a customer number, for example " + command.verb + " " + candidates.get(0).getId());
            }
            return candidates.get(0);
        }
        for (Customer candidate : candidates) {
            if (candidate.getId() == command.customerId) {
                return candidate;
            }
        }
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.getId() == command.customerId) {
                throw new IllegalStateException(describe(customer) + " is " + customer.getStatus()
                        + ": " + ineligible);
            }
        }
        throw new IllegalArgumentException("There is no customer #" + command.customerId);
    }

    private List<Customer> customersWithStatus(CustomerStatus status) {
        List<Customer> matching = new ArrayList<>();
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.getStatus() == status) {
                matching.add(customer);
            }
        }
        return matching;
    }

    private static String describe(Customer customer) {
        return "Customer #" + customer.getId() + " (" + customer.getTypeLabel() + ")";
    }

    private static String idList(Collection<Customer> customers) {
        StringBuilder ids = new StringBuilder();
        for (Customer customer : customers) {
            ids.append(ids.length() == 0 ? "#" : ", #").append(customer.getId());
        }
        return ids.toString();
    }

    /** A manager command: a verb and an optional customer number. A blank line means WAIT. */
    private static final class Command {
        private final String verb;
        private final Integer customerId;

        private Command(String verb, Integer customerId) {
            this.verb = verb;
            this.customerId = customerId;
        }

        private static Command parse(String raw) {
            String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
            if (text.isEmpty()) {
                return new Command("WAIT", null);
            }
            String[] words = text.split("\\s+");
            if (words.length > 2) {
                throw new IllegalArgumentException("Expected 'COMMAND [customer number]' but got '" + raw.trim() + "'");
            }
            Integer id = null;
            if (words.length == 2) {
                String number = words[1].startsWith("#") ? words[1].substring(1) : words[1];
                try {
                    id = Integer.valueOf(number);
                } catch (NumberFormatException notANumber) {
                    throw new IllegalArgumentException("'" + words[1] + "' is not a customer number");
                }
            }
            return new Command(words[0], id);
        }
    }
}
