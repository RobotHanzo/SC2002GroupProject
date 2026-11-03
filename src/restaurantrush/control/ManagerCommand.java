package restaurantrush.control;

import java.util.List;
import java.util.Locale;

import restaurantrush.entity.Customer;

/**
 * One typed manager choice: a verb and an optional customer number, for example "SERVE #3".
 * A blank line means WAIT. It reads the text and works out which customer it refers to; whether
 * the task is allowed is decided by the staff and the domain objects.
 */
final class ManagerCommand {
    private final String verb;
    private final Integer customerId;

    private ManagerCommand(String verb, Integer customerId) {
        this.verb = verb;
        this.customerId = customerId;
    }

    static ManagerCommand parse(String raw) {
        String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (text.isEmpty()) {
            return new ManagerCommand("WAIT", null);
        }
        String[] words = text.split("\\s+");
        if (words.length > 2) {
            throw new IllegalArgumentException("Expected 'COMMAND [customer number]' but got '" + raw.trim() + "'");
        }
        Integer id = null;
        if (words.length == 2) {
            String number = words[1].startsWith("#") ? words[1].substring(1) : words[1];
            try {
                id = Integer.valueOf(number);
            } catch (NumberFormatException notANumber) {
                throw new IllegalArgumentException("'" + words[1] + "' is not a customer number");
            }
        }
        return new ManagerCommand(words[0], id);
    }

    String getVerb() {
        return verb;
    }

    /**
     * Resolves the customer this command refers to among those who could take part in the task, or
     * explains why it cannot. With no number given, a single candidate is chosen automatically.
     */
    Customer select(List<Customer> candidates, List<Customer> everyone, String noCandidates, String ineligible) {
        if (customerId == null) {
            if (candidates.isEmpty()) {
                throw new IllegalStateException("Nothing to do: " + noCandidates);
            }
            if (candidates.size() > 1) {
                throw new IllegalArgumentException("Several customers qualify (" + idList(candidates)
                        + "); add a customer number, for example " + verb + " " + candidates.get(0).getId());
            }
            return candidates.get(0);
        }
        for (Customer candidate : candidates) {
            if (candidate.getId() == customerId) {
                return candidate;
            }
        }
        for (Customer customer : everyone) {
            if (customer.getId() == customerId) {
                throw new IllegalStateException(customer.getDisplayName() + " is " + customer.getStatus()
                        + ": " + ineligible);
            }
        }
        throw new IllegalArgumentException("There is no customer #" + customerId);
    }

    private static String idList(List<Customer> customers) {
        StringBuilder ids = new StringBuilder();
        for (Customer customer : customers) {
            ids.append(ids.length() == 0 ? "#" : ", #").append(customer.getId());
        }
        return ids.toString();
    }
}
