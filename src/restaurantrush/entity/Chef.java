package restaurantrush.entity;

/** The Chef spends one preparation unit on an order per turn (the controller allows one choice per turn). */
public class Chef {

    /** Advances the order by one unit; an order that is READY or cancelled is rejected by the Order. */
    public void prepare(Order order) {
        order.advancePreparation();
    }
}
