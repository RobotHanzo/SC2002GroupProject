package restaurantrush.entity;

/** Where a customer is in the visit: queue, table, order, meal, payment, or gone. */
public enum CustomerStatus {
    WAITING,
    SEATED,
    ORDERED,
    SERVED,
    READY_TO_PAY,
    PAID,
    ABANDONED
}
