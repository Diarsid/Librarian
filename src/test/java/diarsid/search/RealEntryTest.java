package diarsid.search;

import java.util.UUID;

import diarsid.search.impl.model.RealEntry;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RealEntryTest {

    UUID userUuid = randomUUID();
    UUID txUuid = randomUUID();

    @Test
    public void createEntry() {
        String origin = "The Ice-Shirt (Seven Dreams #1), authors: William T. Vollmann";
        RealEntry entry = new RealEntry(origin, userUuid, txUuid);

        assertThat(entry.stringLower()).isEqualTo("the ice shirt seven dreams n1 authors william t vollmann");
    }
}
