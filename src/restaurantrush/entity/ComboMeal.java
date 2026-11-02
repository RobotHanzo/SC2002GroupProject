package restaurantrush.entity;

import java.util.List;

/**
 * A combo is a menu item made of two existing menu items. It costs 90% of their combined price
 * (rounded to the nearest cent) and takes the sum of their preparation units, so the Waiter, Chef
 * and Cashier treat it like any other dish.
 */
public class ComboMeal extends MenuItem {
    private static final int PRICE_PERCENT = 90;

    private final MenuItem first;
    private final MenuItem second;

    public ComboMeal(MenuItem first, MenuItem second) {
        super(first.getName() + " + " + second.getName(),
                ((first.getPriceCents() + second.getPriceCents()) * PRICE_PERCENT + 50) / 100,
                first.getPreparationUnits() + second.getPreparationUnits());
        this.first = first;
        this.second = second;
    }

    public List<MenuItem> getComponents() {
        return List.of(first, second);
    }
}
