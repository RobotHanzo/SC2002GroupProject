package restaurantrush.entity;

/**
 * A demanding customer: loses 12 per waiting turn, always orders the most expensive dish, and is
 * further upset (-10) when served more than 2 turns after the order was taken.
 */
public class CriticCustomer extends Customer {
    private static final int SATISFACTION_LOSS = 12;
    private static final int LATE_SERVICE_TURNS = 2;
    private static final int LATE_PENALTY = 10;

    public CriticCustomer(int customerId) {
        super(customerId);
    }

    @Override
    public String getTypeLabel() {
        return "Critic";
    }

    @Override
    protected int getSatisfactionLoss() {
        return SATISFACTION_LOSS;
    }

    @Override
    public MenuItem chooseMenuItem(Menu menu) {
        return menu.getMostExpensive();
    }

    @Override
    public void onServed(int turnsWaited) {
        if (turnsWaited > LATE_SERVICE_TURNS) {
            adjustSatisfaction(-LATE_PENALTY);
        }
    }
}
