package restaurantrush.entity;

/** The Cashier collects the bill of one READY_TO_PAY customer, records the revenue and frees the table. */
public class Cashier {

    public void processPayment(Customer customer, Restaurant restaurant) {
        Order order = customer.getOrder();
        if (customer.getStatus() != CustomerStatus.READY_TO_PAY || order == null) {
            throw new IllegalStateException("Customer #" + customer.getId() + " is " + customer.getStatus()
                    + "; the Cashier can only charge a READY_TO_PAY customer");
        }
        restaurant.recordPayment(order.getBillCents());
        customer.markPaid();
        order.markPaid();
        restaurant.releaseTableOf(customer);
    }
}
