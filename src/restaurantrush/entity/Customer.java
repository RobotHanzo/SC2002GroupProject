package restaurantrush.entity;

/**
 * A restaurant guest. Every kind of customer follows the same order and service flow; subclasses
 * differ only in the hooks below (loss per waiting turn, billing, food choice, reaction to being
 * served), so the controller and staff call these on Customer and never check the type.
 * Satisfaction and status are private and change only through the methods of this class.
 * Lifecycle: WAITING -> SEATED -> ORDERED -> SERVED -> READY_TO_PAY -> PAID, or ABANDONED.
 */
public abstract class Customer {
    private static final int MAX_SATISFACTION = 100;

    private final int customerId;
    private int satisfaction = MAX_SATISFACTION;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private int servedTurn;
    private int readyTurn;

    protected Customer(int customerId) {
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

    /** Short name of the customer type, for display. */
    public abstract String getTypeLabel();

    /** Satisfaction lost at the end of each turn spent waiting for service. */
    protected abstract int getSatisfactionLoss();

    /** Documented food rule: menu entry number (customerId mod menu size). The Critic overrides it. */
    public MenuItem chooseMenuItem(Menu menu) {
        return menu.getItem(customerId % menu.size());
    }

    /** The bill for a dish at the given list price. The VIP overrides it to apply a discount. */
    public int calculateBillCents(int basePriceCents) {
        return basePriceCents;
    }

    /** Called when the Waiter serves this customer; turnsWaited counts turns since the order was taken. */
    public void onServed(int turnsWaited) {
    }

    /** Only subclasses may move satisfaction directly; the value always stays within 0..100. */
    protected void adjustSatisfaction(int delta) {
        satisfaction = Math.max(0, Math.min(MAX_SATISFACTION, satisfaction + delta));
    }

    /** End-of-turn loss for a customer who has not been served yet; everyone else loses nothing. */
    public void reduceSatisfaction() {
        if (isAwaitingService()) {
            adjustSatisfaction(-getSatisfactionLoss());
        }
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
