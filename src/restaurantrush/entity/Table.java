package restaurantrush.entity;

/** A table that holds at most one customer. It stays occupied until that customer pays or leaves. */
public class Table {
    private final int tableId;
    private Customer occupant;

    public Table(int tableId) {
        this.tableId = tableId;
    }

    public int getTableId() {
        return tableId;
    }

    public boolean isAvailable() {
        return occupant == null;
    }

    public void seat(Customer customer) {
        if (!isAvailable()) {
            throw new IllegalStateException("Table " + tableId + " is already occupied");
        }
        occupant = customer;
    }

    public void release() {
        occupant = null;
    }

    public Customer getOccupied() {
        return occupant;
    }
}
