package diarsid.search.api.model.meta;

import java.time.LocalDateTime;

public interface Identifiable extends Storable, Unique {

    LocalDateTime time();
}
