package net.mjdawson.bracketchat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandInputTest {
    @Test void replacesEveryVanillaMessagingAlias() {
        String[][] cases = {{"w", "bmsg"}, {"msg", "bmsg"}, {"tell", "bmsg"},
            {"say", "bsay"}, {"me", "bme"}, {"teammsg", "bteammsg"}, {"tm", "bteammsg"}};
        for (String[] pair : cases) {
            for (String namespace : new String[] {"", "minecraft:", "bukkit:"}) {
                assertEquals("bracketchat:" + pair[1] + " Max hello  world",
                    CommandInput.replacement("/" + namespace + pair[0] + " Max hello  world"));
            }
        }
    }
    @Test void doesNotCaptureOtherCommandsOrRecurse() {
        for (String input : new String[] {"/tellraw @a {}", "/weather clear", "/team list",
            "/bracketchat:bmsg Max hi", "/essentials:msg Max hi", "/message Max hi", ""})
            assertNull(CommandInput.replacement(input));
    }
    @Test void acceptsConsoleCaseAndMissingArguments() {
        assertEquals("bracketchat:bsay Hello", CommandInput.replacement("SAY Hello"));
        assertEquals("bracketchat:bmsg", CommandInput.replacement("/w"));
    }
    @Test void preservesQuotedSelectorAndMessage() {
        assertArrayEquals(new String[] {"@a[name=\"A B\", scores={x=1..}]", "hello  world"},
            CommandInput.firstArgument("@a[name=\"A B\", scores={x=1..}] hello  world"));
        assertArrayEquals(new String[] {"Max", "hello [everyone] <red>literal</red>"},
            CommandInput.firstArgument(" Max   hello [everyone] <red>literal</red> "));
    }
}
