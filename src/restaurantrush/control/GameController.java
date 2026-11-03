package restaurantrush.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import restaurantrush.boundary.RestaurantCLI;
import restaurantrush.entity.Customer;
import restaurantrush.entity.HappyHour;
import restaurantrush.entity.Money;
import restaurantrush.entity.Restaurant;

/**
 * Runs the game: 18 turns of six steps each (arrivals, manager choices, kitchen and serving,
 * eating and payment, waiting and abandonment, result). It owns the turn counter and the order of
 * the steps. Reading a choice is the CLI's job, carrying it out is StaffActions' job and deciding
 * whether it is legal is the domain objects' job; the controller only sequences them.
 */
public class GameController {
    private static final int MAX_TURNS = 18;

    private final Restaurant restaurant;
    private final ArrivalSchedule schedule;
    private final StaffActions staff;
    private int currentTurn;
    private final int maxTurns;
    private final RestaurantCLI cli;

    public GameController(RestaurantCLI cli) {
        this.cli = cli;
        this.restaurant = new Restaurant();
        this.schedule = new ArrivalSchedule();
        this.staff = new StaffActions(restaurant);
        this.maxTurns = MAX_TURNS;
    }

    /** Plays all turns (there is no early ending), then reports victory or defeat. */
    public void startGame() {
        cli.displayIntro(maxTurns);
        while (currentTurn < maxTurns) {
            currentTurn++;
            executeTurn();
        }
        cli.displayResult(restaurant.hasMetObjective(), restaurant, currentTurn);
    }

    private void executeTurn() {
        processScheduledArrival();
        cli.displayTurnStatus(restaurant, currentTurn, maxTurns);
        processHappyHourActivation();
        applyHappyHourRecovery();
        coordinateHostAction();
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

    /** Step 2 (Host): at most one queued customer is seated, on top of the Waiter's own task. */
    private void coordinateHostAction() {
        chooseUntilAccepted(cli::readHostChoice, staff::host);
    }

    /** Steps 2 and 3: the Waiter acts first, then the Chef. A rejected choice does not use up the task. */
    private void coordinateStaffActions() {
        chooseUntilAccepted(cli::readWaiterChoice, command -> staff.waiter(command, currentTurn));
        chooseUntilAccepted(cli::readChefChoice, staff::chef);
    }

    private void chooseUntilAccepted(Supplier<String> reader, Function<ManagerCommand, Optional<String>> task) {
        while (true) {
            String choice = reader.get();
            try {
                task.apply(ManagerCommand.parse(choice)).ifPresent(cli::displayMessage);
                return;
            } catch (IllegalArgumentException | IllegalStateException rejected) {
                cli.displayError(rejected.getMessage());
            }
        }
    }

    /** Step 4: served customers finish eating, then the Cashier charges at most one READY_TO_PAY customer. */
    private void coordinatePaymentProcessing() {
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.canFinishEating(currentTurn)) {
                customer.markReadyToPay(currentTurn);
                cli.displayMessage(customer.getDisplayName() + " finishes eating and is ready to pay.");
            }
        }
        Customer payer = restaurant.findOldestEligiblePayer(currentTurn);
        if (payer != null) {
            int bill = payer.getOrder().getBillCents();
            restaurant.getCashier().processPayment(payer, restaurant);
            cli.displayMessage("Cashier collects " + Money.format(bill) + " from " + payer.getDisplayName()
                    + " (satisfaction " + payer.getSatisfaction() + "). The table is free again.");
        }
    }

    /** Step 5: customers who are still waiting lose satisfaction; at 0 they leave without paying. */
    private void processEndOfTurnUpdates() {
        for (Customer customer : restaurant.getCustomers()) {
            if (!customer.isAwaitingService()) {
                continue;
            }
            customer.reduceSatisfaction();
            if (customer.shouldAbandon()) {
                restaurant.handleAbandonment(customer);
                cli.displayMessage(customer.getDisplayName()
                        + " runs out of patience and leaves without paying. Any unfinished order is cancelled.");
            }
        }
        restaurant.getHappyHour().endTurn();
    }
}
