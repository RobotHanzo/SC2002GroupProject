package restaurantrush.entity;

/**
 * The Host seats at most one queued customer per turn, in addition to whatever the Waiter does
 * (the Waiter can still seat customers too). The controller allows one Host task per turn.
 */
public class Host {

    /** Rejects an occupied table or a customer who is not in the waiting queue, before changing anything. */
    public void seatCustomer(Customer customer, Table table) {
        if (!table.isAvailable()) {
            throw new IllegalStateException("Table " + table.getTableId() + " is already occupied");
        }
        customer.markSeated();
        table.seat(customer);
    }
}
