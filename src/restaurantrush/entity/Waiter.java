package restaurantrush.entity;

/**
 * The Waiter seats customers, takes their orders and serves finished dishes. The controller lets
 * the Waiter do at most one of these per turn; each method rejects an impossible request by
 * throwing, before anything is changed.
 */
public class Waiter {

    public void seatCustomer(Customer customer, Table table) {
        if (customer.getStatus() != CustomerStatus.WAITING) {
            throw new IllegalStateException("Customer #" + customer.getId() + " is " + customer.getStatus()
                    + "; only a customer in the waiting queue can be seated");
        }
        if (!table.isAvailable()) {
            throw new IllegalStateException("Table " + table.getTableId() + " is already occupied");
        }
        table.seat(customer);
        customer.markSeated();
    }

    /**
     * The customer chooses the dish and works out their own bill (a VIP gets a discount); then the
     * Happy Hour discount, if active, is applied on top. The bill is locked in the new Order.
     */
    public Order takeOrder(Customer customer, Menu menu, HappyHour happyHour, int currentTurn) {
        if (customer.getStatus() != CustomerStatus.SEATED) {
            throw new IllegalStateException("Customer #" + customer.getId() + " is " + customer.getStatus()
                    + "; only a seated customer can order");
        }
        MenuItem item = customer.chooseMenuItem(menu);
        int billCents = happyHour.applyDiscount(customer.calculateBillCents(item.getPriceCents()));
        Order order = new Order(item, billCents, currentTurn);
        customer.assignOrder(order);
        return order;
    }

    public void serveOrder(Customer customer, int currentTurn) {
        Order order = customer.getOrder();
        if (customer.getStatus() != CustomerStatus.ORDERED || order == null) {
            throw new IllegalStateException("Customer #" + customer.getId() + " is " + customer.getStatus()
                    + "; there is no order waiting to be served");
        }
        if (!order.isReady()) {
            throw new IllegalStateException("Customer #" + customer.getId() + "'s " + order.getItem().getName()
                    + " is not READY yet (" + order.getStatus() + ", " + order.getCompletedUnits() + "/"
                    + order.getItem().getPreparationUnits() + " units)");
        }
        order.markServed();
        customer.markServed(currentTurn);
        customer.onServed(currentTurn - order.getTurnTaken());
    }
}
