package restaurantrush.entity;

/**
 * One customer's order. The status is private and moves only through the methods below, so an
 * illegal jump (for example serving a dish that is not READY) is rejected here, not in the UI.
 * Legal: PLACED -> PREPARING -> READY -> SERVED -> PAID, with PLACED -> READY for a 1-unit dish,
 * and PLACED / PREPARING / READY -> CANCELLED when the customer leaves.
 */
public class Order {
    private final MenuItem item;
    private int completedUnits;
    private OrderStatus status;
    private final int billCents;
    private final int turnTaken;

    /** The bill is fixed here and has no setter: the price is locked when the order is taken. */
    public Order(MenuItem item, int billCents, int turnTaken) {
        this.item = item;
        this.billCents = billCents;
        this.turnTaken = turnTaken;
        this.status = OrderStatus.PLACED;
    }

    /** The Chef spends one preparation unit; the last unit makes the order READY. */
    public void advancePreparation() {
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING) {
            throw new IllegalStateException("The " + item.getName() + " order is " + status
                    + " and cannot be prepared any further");
        }
        completedUnits++;
        status = completedUnits >= item.getPreparationUnits() ? OrderStatus.READY : OrderStatus.PREPARING;
    }

    public boolean isReady() {
        return status == OrderStatus.READY;
    }

    public void markServed() {
        require(OrderStatus.READY, "served");
        status = OrderStatus.SERVED;
    }

    public void markPaid() {
        require(OrderStatus.SERVED, "paid");
        status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING && status != OrderStatus.READY) {
            throw new IllegalStateException("A " + status + " order cannot be cancelled");
        }
        status = OrderStatus.CANCELLED;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public MenuItem getItem() {
        return item;
    }

    public int getCompletedUnits() {
        return completedUnits;
    }

    public int getBillCents() {
        return billCents;
    }

    public int getTurnTaken() {
        return turnTaken;
    }

    private void require(OrderStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("The " + item.getName() + " order is " + status
                    + "; only a " + expected + " order can be " + action);
        }
    }
}
