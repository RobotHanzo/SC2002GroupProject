package restaurantrush.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The dishes customers can order. Customers choose from it; the manager never does. */
public class Menu {
    private final List<MenuItem> items;

    public Menu(List<MenuItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A menu needs at least one item");
        }
        this.items = new ArrayList<>(items);
    }

    public List<MenuItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public MenuItem getItem(int index) {
        return items.get(index);
    }

    public int size() {
        return items.size();
    }

    /** The item with the highest price (the first one listed if prices tie). */
    public MenuItem getMostExpensive() {
        MenuItem best = items.get(0);
        for (MenuItem item : items) {
            if (item.getPriceCents() > best.getPriceCents()) {
                best = item;
            }
        }
        return best;
    }
}
