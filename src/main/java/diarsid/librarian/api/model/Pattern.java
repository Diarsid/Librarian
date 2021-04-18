package diarsid.librarian.api.model;

import diarsid.librarian.api.model.meta.UserScoped;
import diarsid.support.model.CreatedAt;
import diarsid.support.model.Storable;
import diarsid.support.model.Unique;

public interface Pattern extends Unique, Storable, CreatedAt, UserScoped {

    String string();
}
