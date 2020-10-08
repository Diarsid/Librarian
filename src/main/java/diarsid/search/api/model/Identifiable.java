package diarsid.search.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

public interface Identifiable extends Storable {

    UUID uuid();

    LocalDateTime time();
}
