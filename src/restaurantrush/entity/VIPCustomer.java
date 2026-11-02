package restaurantrush.entity;

/** A VIP is more patient (loses 5 per waiting turn) and pays 90% of the list price. */
public class VIPCustomer extends Customer {
    private static final int SATISFACTION_LOSS = 5;
    private static final int PRICE_PERCENT = 90;

    public VIPCustomer(int customerId) {
        super(customerId);
    }

    @Override
    public String getTypeLabel() {
        return "VIP";
    }

    @Override
    protected int getSatisfactionLoss() {
        return SATISFACTION_LOSS;
    }

    /** 10% off, rounded to the nearest cent. */
    @Override
    public int calculateBillCents(int basePriceCents) {
        return (basePriceCents * PRICE_PERCENT + 50) / 100;
    }
}
