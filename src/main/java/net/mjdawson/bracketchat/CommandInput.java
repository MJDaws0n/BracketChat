package net.mjdawson.bracketchat;

import java.util.Locale;
import java.util.Map;

/** Pure parsing shared by command interception and tests. */
final class CommandInput {
    private static final Map<String, String> ROUTES = Map.ofEntries(
        Map.entry("w", "bmsg"), Map.entry("msg", "bmsg"), Map.entry("tell", "bmsg"),
        Map.entry("say", "bsay"), Map.entry("me", "bme"),
        Map.entry("teammsg", "bteammsg"), Map.entry("tm", "bteammsg"));

    static String replacement(String raw) {
        String command = raw.startsWith("/") ? raw.substring(1) : raw;
        String[] parts = firstArgument(command);
        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            String namespace = label.substring(0, colon);
            if (!namespace.equals("minecraft") && !namespace.equals("bukkit")) return null;
            label = label.substring(colon + 1);
        }
        String destination = ROUTES.get(label);
        return destination == null ? null : "bracketchat:" + destination
            + (parts[1].isBlank() ? "" : " " + parts[1]);
    }

    /** Keep selectors with spaces inside brackets/quotes intact. */
    static String[] firstArgument(String input) {
        String text = input.strip();
        int depth = 0;
        char quote = 0;
        boolean escape = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) { escape = false; continue; }
            if (quote != 0) {
                if (c == '\\') escape = true;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') quote = c;
            else if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') depth--;
            else if (Character.isWhitespace(c) && depth == 0)
                return new String[] {text.substring(0, i), text.substring(i).stripLeading()};
        }
        return new String[] {text, ""};
    }
}
