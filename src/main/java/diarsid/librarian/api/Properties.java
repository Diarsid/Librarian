package diarsid.librarian.api;

import java.time.LocalDateTime;

import diarsid.librarian.api.model.User;

public interface Properties {

    LocalDateTime lastModificationTime(User user);
}
