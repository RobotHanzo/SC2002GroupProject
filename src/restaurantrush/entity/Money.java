package restaurantrush.entity;

import java.util.Locale;

/** Money is kept as integer cents everywhere; this only formats it as dollars with two decimals. */
public final class Money {
    private Money() {
    }

    public static String format(int cents) {
        return String.format(Locale.ROOT, "$%d.%02d", cents / 100, cents % 100);
    }
}
