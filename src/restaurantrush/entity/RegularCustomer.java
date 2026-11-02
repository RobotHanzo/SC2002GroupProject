package restaurantrush.entity;

/** The baseline customer: full price, loses 8 satisfaction per waiting turn. */
public class RegularCustomer extends Customer {
    private static final int SATISFACTION_LOSS = 8;

    public RegularCustomer(int customerId) {
        super(customerId);
    }

    @Override
    public String getTypeLabel() {
        return "Regular";
    }

    @Override
    protected int getSatisfactionLoss() {
        return SATISFACTION_LOSS;
    }
}
