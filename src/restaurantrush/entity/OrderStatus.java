package restaurantrush.entity;

/** Where an order is between the Waiter taking it and the Cashier collecting payment. */
public enum OrderStatus {
    PLACED,
    PREPARING,
    READY,
    SERVED,
    PAID,
    CANCELLED
}
