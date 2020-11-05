package diarsid.search.api;

import java.util.Optional;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;

public interface Labels {

    Entry.Label getOrSave(User user, String name);

    Optional<Entry.Label> findLabelBy(User user, String name);
}
