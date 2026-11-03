package restaurantrush.control;

import java.util.Optional;
import java.util.function.BiConsumer;

import restaurantrush.entity.Customer;
import restaurantrush.entity.CustomerStatus;
import restaurantrush.entity.Money;
import restaurantrush.entity.MenuItem;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Table;

/**
 * Carries out a manager's command with the Host, the Waiter or the Chef. Each method picks the
 * customer the command refers to, asks the staff member to do the task and returns the sentence
 * for the screen (empty for WAIT). An impossible command throws before anything is changed, so
 * the controller can ask again without the staff member having used the task.
 */
final class StaffActions {
    private final Restaurant restaurant;

    StaffActions(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    Optional<String> host(ManagerCommand command) {
        return switch (command.getVerb()) {
            case "WAIT" -> Optional.empty();
            case "SEAT" -> Optional.of(seat(command, restaurant.getHost()::seatCustomer, "Host"));
            default -> throw new IllegalArgumentException(
                    "Unknown Host command '" + command.getVerb() + "'. Use SEAT or WAIT.");
        };
    }

    Optional<String> waiter(ManagerCommand command, int currentTurn) {
        return switch (command.getVerb()) {
            case "WAIT" -> Optional.empty();
            case "SEAT" -> Optional.of(seat(command, restaurant.getWaiter()::seatCustomer, "Waiter"));
            case "TAKE_ORDER", "ORDER", "TAKE" -> Optional.of(takeOrder(command, currentTurn));
            case "SERVE" -> Optional.of(serve(command, currentTurn));
            default -> throw new IllegalArgumentException(
                    "Unknown Waiter command '" + command.getVerb() + "'. Use SEAT, TAKE_ORDER, SERVE or WAIT.");
        };
    }

    Optional<String> chef(ManagerCommand command) {
        return switch (command.getVerb()) {
            case "WAIT" -> Optional.empty();
            case "COOK", "PREPARE", "ADVANCE" -> Optional.of(cook(command));
            default -> throw new IllegalArgumentException(
                    "Unknown Chef command '" + command.getVerb() + "'. Use COOK or WAIT.");
        };
    }

    private String seat(ManagerCommand command, BiConsumer<Customer, Table> seater, String actor) {
        Customer customer = command.select(restaurant.getWaitingQueue(), restaurant.getCustomers(),
                "nobody is in the waiting queue", "only a customer in the waiting queue can be seated");
        Table table = restaurant.findAvailableTable();
        if (table == null) {
            throw new IllegalStateException("Both tables are occupied; a table frees up when its customer pays or leaves");
        }
        restaurant.seatFromQueue(customer, table, seater);
        return actor + " seats " + customer.getDisplayName() + " at Table " + table.getTableId() + ".";
    }

    private String takeOrder(ManagerCommand command, int currentTurn) {
        Customer customer = command.select(restaurant.getCustomersWithStatus(CustomerStatus.SEATED),
                restaurant.getCustomers(), "no seated customer is waiting to order", "only a seated customer can order");
        Order order = restaurant.getWaiter().takeOrder(customer, restaurant.getMenu(), restaurant.getHappyHour(), currentTurn);
        MenuItem item = order.getItem();
        return "Waiter takes the order of " + customer.getDisplayName() + ". They choose "
                + item.getName() + " (list price " + Money.format(item.getPriceCents()) + ", "
                + item.getPreparationUnits() + " preparation unit(s)). Bill locked at "
                + Money.format(order.getBillCents()) + ".";
    }

    private String serve(ManagerCommand command, int currentTurn) {
        Customer customer = command.select(restaurant.getCustomersWithStatus(CustomerStatus.ORDERED),
                restaurant.getCustomers(), "no customer has an order to serve",
                "only a customer who has ordered can be served");
        restaurant.getWaiter().serveOrder(customer, currentTurn);
        return "Waiter serves the " + customer.getOrder().getItem().getName() + " to "
                + customer.getDisplayName() + ".";
    }

    private String cook(ManagerCommand command) {
        Customer customer = command.select(restaurant.getCustomersWithStatus(CustomerStatus.ORDERED),
                restaurant.getCustomers(), "no order has been taken yet",
                "only a customer who has ordered has food to prepare");
        Order order = customer.getOrder();
        restaurant.getChef().prepare(order);
        return "Chef works on the " + order.getItem().getName() + " for " + customer.getDisplayName()
                + ": " + order.getCompletedUnits() + "/" + order.getItem().getPreparationUnits()
                + " units, order is " + order.getStatus() + ".";
    }
}
