package diarsid.search.api;

import java.time.LocalDateTime;

import diarsid.search.api.model.User;

public interface Properties {

    LocalDateTime lastModificationTime(User user);
}
