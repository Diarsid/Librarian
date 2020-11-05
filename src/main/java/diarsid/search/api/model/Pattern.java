package diarsid.search.api.model;

import diarsid.search.api.model.meta.Identifiable;
import diarsid.search.api.model.meta.UserScoped;

public interface Pattern extends Identifiable, UserScoped {

    String string();
}
