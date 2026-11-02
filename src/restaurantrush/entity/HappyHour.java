package restaurantrush.entity;

/**
 * A pricing policy the manager may switch on once per game, at the start of a turn. It lasts for
 * that turn and the next. While active, orders taken get 20% off and customers still waiting for
 * a table or food recover 15 satisfaction at the start of the turn. It never changes an order
 * that is already placed, because the bill is locked in the Order.
 */
public class HappyHour {
    private static final int DISCOUNT_PERCENT = 20;
    private static final int RECOVERY = 15;
    private static final int DURATION = 2;

    private boolean used;
    private int remainingTurns;

    /** Switches Happy Hour on. Returns false (and changes nothing) if it was already used this game. */
    public boolean activate() {
        if (used) {
            return false;
        }
        used = true;
        remainingTurns = DURATION;
        return true;
    }

    public boolean isActive() {
        return remainingTurns > 0;
    }

    /** True until the manager switches Happy Hour on; it cannot be used a second time. */
    public boolean isAvailable() {
        return !used;
    }

    public int getRemainingTurns() {
        return remainingTurns;
    }

    /** The bill after the Happy Hour discount (rounded to the nearest cent), or unchanged when it is not active. */
    public int applyDiscount(int cents) {
        if (!isActive()) {
            return cents;
        }
        return (cents * (100 - DISCOUNT_PERCENT) + 50) / 100;
    }

    /** Satisfaction a waiting customer recovers at the start of an active turn; 0 when not active. */
    public int getRecoveryAmount() {
        return isActive() ? RECOVERY : 0;
    }

    /** Called once at the end of every turn. */
    public void endTurn() {
        if (remainingTurns > 0) {
            remainingTurns--;
        }
    }
}
