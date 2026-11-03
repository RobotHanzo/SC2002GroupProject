package restaurantrush.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The restaurant owns its tables, staff and menu (composition) and keeps the books: the waiting
 * queue, every customer who has arrived, and the revenue. Customers are aggregated because they
 * arrive and leave. It also knows what each staff member can do right now (the getCustomers...
 * queries below), so the screen and the controller ask it instead of re-deriving the rules.
 */
public class Restaurant {
    public static final int REQUIRED_PAID_CUSTOMERS = 4;
    public static final int REQUIRED_REVENUE_CENTS = 4500;
    public static final int REQUIRED_AVERAGE_SATISFACTION = 60;

    private static final int TABLE_COUNT = 2;

    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Customer> waitingQueue = new ArrayList<>();
    private final Waiter waiter = new Waiter();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();
    private final Host host = new Host();
    private final Menu menu;
    private final HappyHour happyHour = new HappyHour();
    private int revenueCents;

    public Restaurant() {
        for (int id = 1; id <= TABLE_COUNT; id++) {
            tables.add(new Table(id));
        }
        MenuItem salad = new MenuItem("Salad", 900, 1);
        MenuItem burger = new MenuItem("Burger", 1500, 1);
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        // The combos are registered once here; customers pick from the whole menu.
        menu = new Menu(List.of(salad, burger, pasta, new ComboMeal(salad, burger), new ComboMeal(salad, pasta)));
    }

    /** A newly arrived customer joins the back of the waiting queue. */
    public void registerArrival(Customer customer) {
        customers.add(customer);
        waitingQueue.add(customer);
    }

    /** The first free table, or null when both are occupied. */
    public Table findAvailableTable() {
        for (Table table : tables) {
            if (table.isAvailable()) {
                return table;
            }
        }
        return null;
    }

    /** The READY_TO_PAY customer who arrived first and may be charged this turn, or null. */
    public Customer findOldestEligiblePayer(int turn) {
        for (Customer customer : customers) {
            if (customer.canPay(turn)) {
                return customer;
            }
        }
        return null;
    }

    public Waiter getWaiter() {
        return waiter;
    }

    public Chef getChef() {
        return chef;
    }

    public Cashier getCashier() {
        return cashier;
    }

    public Host getHost() {
        return host;
    }

    public List<Table> getTables() {
        return Collections.unmodifiableList(tables);
    }

    public Menu getMenu() {
        return menu;
    }

    public HappyHour getHappyHour() {
        return happyHour;
    }

    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    /** The waiting queue, first to arrive first. Read-only: a customer leaves it by being seated or by abandoning. */
    public List<Customer> getWaitingQueue() {
        return Collections.unmodifiableList(waitingQueue);
    }

    /**
     * Seats a queued customer by the given staff action (the Waiter's or the Host's seatCustomer).
     * The customer leaves the queue only if the staff member accepts the request, so a rejected
     * seating changes nothing.
     */
    public void seatFromQueue(Customer customer, Table table, BiConsumer<Customer, Table> seatedBy) {
        if (!waitingQueue.contains(customer)) {
            throw new IllegalStateException("Customer #" + customer.getId() + " is not in the waiting queue");
        }
        seatedBy.accept(customer, table);
        waitingQueue.remove(customer);
    }

    /** Customers with the given status, in arrival order. */
    public List<Customer> getCustomersWithStatus(CustomerStatus status) {
        List<Customer> matching = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.getStatus() == status) {
                matching.add(customer);
            }
        }
        return matching;
    }

    /** Whom the Host or the Waiter could seat now: the queue, or nobody while both tables are occupied. */
    public List<Customer> getSeatableCustomers() {
        return findAvailableTable() == null ? List.of() : List.copyOf(waitingQueue);
    }

    /** Customers whose dish is READY and waiting for the Waiter to serve it. */
    public List<Customer> getCustomersToServe() {
        List<Customer> matching = new ArrayList<>();
        for (Customer customer : getCustomersWithStatus(CustomerStatus.ORDERED)) {
            if (customer.getOrder().isReady()) {
                matching.add(customer);
            }
        }
        return matching;
    }

    /** Customers whose dish still needs the Chef's preparation units. */
    public List<Customer> getCustomersToCook() {
        List<Customer> matching = new ArrayList<>();
        for (Customer customer : getCustomersWithStatus(CustomerStatus.ORDERED)) {
            if (!customer.getOrder().isReady()) {
                matching.add(customer);
            }
        }
        return matching;
    }

    /** Victory needs all three targets (checked after the last turn). */
    public boolean hasMetObjective() {
        return getPaidCustomerCount() >= REQUIRED_PAID_CUSTOMERS
                && getRevenueCents() >= REQUIRED_REVENUE_CENTS
                && getAverageSatisfaction() >= REQUIRED_AVERAGE_SATISFACTION;
    }

    public void recordPayment(int amountCents) {
        if (amountCents < 0) {
            throw new IllegalArgumentException("A payment cannot be negative");
        }
        revenueCents += amountCents;
    }

    public void releaseTableOf(Customer customer) {
        for (Table table : tables) {
            if (table.getOccupied() == customer) {
                table.release();
            }
        }
    }

    public int getPaidCustomerCount() {
        return countWithStatus(CustomerStatus.PAID);
    }

    public int getServedCustomerCount() {
        return countWithStatus(CustomerStatus.SERVED) + countWithStatus(CustomerStatus.READY_TO_PAY)
                + countWithStatus(CustomerStatus.PAID);
    }

    /** Mean satisfaction of paid customers, as recorded when each paid; 0 when nobody has paid. */
    public double getAverageSatisfaction() {
        int total = 0;
        int paid = 0;
        for (Customer customer : customers) {
            if (customer.getStatus() == CustomerStatus.PAID) {
                total += customer.getSatisfaction();
                paid++;
            }
        }
        return paid == 0 ? 0.0 : (double) total / paid;
    }

    public int getRevenueCents() {
        return revenueCents;
    }

    /** A customer whose satisfaction hit 0 leaves: cancel the unfinished order, free the table, leave the queue. */
    public void handleAbandonment(Customer customer) {
        customer.markAbandoned();
        Order order = customer.getOrder();
        if (order != null) {
            order.cancel();
        }
        releaseTableOf(customer);
        waitingQueue.remove(customer);
    }

    public int getUnhappyDepartureCount() {
        return countWithStatus(CustomerStatus.ABANDONED);
    }

    private int countWithStatus(CustomerStatus status) {
        int count = 0;
        for (Customer customer : customers) {
            if (customer.getStatus() == status) {
                count++;
            }
        }
        return count;
    }
}
