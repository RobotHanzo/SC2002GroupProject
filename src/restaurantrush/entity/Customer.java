package restaurantrush.entity;

import java.util.List;

/**
 * A restaurant guest. Stage 1 has one kind of customer: a regular who starts at satisfaction 100
 * and loses 8 for every turn spent waiting. Satisfaction and status are private and change only
 * through the methods below.
 * Lifecycle: WAITING -> SEATED -> ORDERED -> SERVED -> READY_TO_PAY -> PAID, or ABANDONED.
 */
public class Customer {
    private static final int MAX_SATISFACTION = 100;
    private static final int SATISFACTION_LOSS = 8;

    private final int customerId;
    private int satisfaction = MAX_SATISFACTION;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private int servedTurn;
    private int readyTurn;

    public Customer(int customerId) {
        this.customerId = customerId;
    }

    public int getId() {
        return customerId;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public int getSatisfaction() {
        return satisfaction;
    }

    /** Documented food rule: the customer takes menu entry number (customerId mod menu size). */
    public MenuItem chooseMenuItem(List<MenuItem> items) {
        return items.get(customerId % items.size());
    }

    public void assignOrder(Order newOrder) {
        require(CustomerStatus.SEATED);
        order = newOrder;
        status = CustomerStatus.ORDERED;
    }

    public Order getOrder() {
        return order;
    }

    public void markSeated() {
        require(CustomerStatus.WAITING);
        status = CustomerStatus.SEATED;
    }

    public void markServed(int turn) {
        require(CustomerStatus.ORDERED);
        servedTurn = turn;
        status = CustomerStatus.SERVED;
    }

    /** A served customer eats for one full turn: allowed from the turn after the serving turn. */
    public boolean canFinishEating(int turn) {
        return status == CustomerStatus.SERVED && turn > servedTurn;
    }

    public void markReadyToPay(int turn) {
        if (!canFinishEating(turn)) {
            throw new IllegalStateException("Customer #" + customerId + " has not finished eating yet");
        }
        readyTurn = turn;
        status = CustomerStatus.READY_TO_PAY;
    }

    /** The Cashier may charge from the turn after the customer became READY_TO_PAY. */
    public boolean canPay(int turn) {
        return status == CustomerStatus.READY_TO_PAY && turn > readyTurn;
    }

    public void markPaid() {
        require(CustomerStatus.READY_TO_PAY);
        status = CustomerStatus.PAID;
    }

    /** End-of-turn loss for a customer who has not been served yet; everyone else loses nothing. */
    public void reduceSatisfaction() {
        if (isAwaitingService()) {
            satisfaction = Math.max(0, satisfaction - SATISFACTION_LOSS);
        }
    }

    public boolean shouldAbandon() {
        return isAwaitingService() && satisfaction <= 0;
    }

    public void markAbandoned() {
        if (!isAwaitingService()) {
            throw new IllegalStateException("Customer #" + customerId + " is " + status + " and cannot abandon");
        }
        status = CustomerStatus.ABANDONED;
    }

    private boolean isAwaitingService() {
        return status == CustomerStatus.WAITING || status == CustomerStatus.SEATED
                || status == CustomerStatus.ORDERED;
    }

    private void require(CustomerStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Customer #" + customerId + " is " + status + ", not " + expected);
        }
    }
}
