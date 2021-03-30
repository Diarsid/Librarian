package diarsid.search.api.model;

import diarsid.support.model.CreatedAt;
import diarsid.support.model.Storable;
import diarsid.support.model.Unique;

public interface User extends Unique, Storable, CreatedAt {

    String name();

}
