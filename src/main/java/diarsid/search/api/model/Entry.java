package diarsid.search.api.model;

import java.util.List;

public interface Entry extends Identifiable, UserScoped {

    interface Label extends Identifiable, UserScoped {

        String name();
    }

    String string();

    List<Label> labels();

}
