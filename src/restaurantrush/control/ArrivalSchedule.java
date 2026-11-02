package restaurantrush.control;

import java.util.List;
import java.util.function.IntFunction;

import restaurantrush.entity.CriticCustomer;
import restaurantrush.entity.Customer;
import restaurantrush.entity.RegularCustomer;
import restaurantrush.entity.VIPCustomer;

/**
 * The base-setting arrivals: one customer at the start of turns 1, 4, 7, 10, 13 and 16. Customers
 * are numbered in arrival order. Two of the six are special: the turn-1 arrival is a VIP and the
 * turn-10 arrival is a Critic; the other four are Regulars.
 */
public class ArrivalSchedule {
    private final int[] arrivalTurns = {1, 4, 7, 10, 13, 16};
    private final List<IntFunction<Customer>> customerTypes = List.of(
            VIPCustomer::new, RegularCustomer::new, RegularCustomer::new,
            CriticCustomer::new, RegularCustomer::new, RegularCustomer::new);
    private int nextCustomerId = 1;

    public boolean isArrivalTurn(int turn) {
        return indexOf(turn) >= 0;
    }

    /** Creates the customer who arrives at the start of this turn. */
    public Customer createArrival(int turn) {
        int index = indexOf(turn);
        if (index < 0) {
            throw new IllegalArgumentException("Nobody arrives at the start of turn " + turn);
        }
        return customerTypes.get(index).apply(nextCustomerId++);
    }

    private int indexOf(int turn) {
        for (int i = 0; i < arrivalTurns.length; i++) {
            if (arrivalTurns[i] == turn) {
                return i;
            }
        }
        return -1;
    }
}
