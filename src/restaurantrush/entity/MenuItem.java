package restaurantrush.entity;

/** A dish on the menu. Immutable: name, price in cents and the preparation units the Chef must spend. */
public final class MenuItem {
    private final String name;
    private final int priceCents;
    private final int preparationUnits;

    public MenuItem(String name, int priceCents, int preparationUnits) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A menu item needs a name");
        }
        if (priceCents <= 0 || preparationUnits <= 0) {
            throw new IllegalArgumentException("Price and preparation units must be positive");
        }
        this.name = name;
        this.priceCents = priceCents;
        this.preparationUnits = preparationUnits;
    }

    public String getName() {
        return name;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getPreparationUnits() {
        return preparationUnits;
    }
}
