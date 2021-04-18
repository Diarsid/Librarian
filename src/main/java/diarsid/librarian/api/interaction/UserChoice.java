package diarsid.librarian.api.interaction;

import diarsid.librarian.api.annotations.ImplementationRequired;
import diarsid.support.objects.CommonEnum;

@ImplementationRequired
public interface UserChoice {

    enum Decision implements CommonEnum<Decision> {
        DONE,
        NOT_DONE,
        REJECTION
    }

    Decision decision();

    /*
    < 0 when this.type() returns REJECTION or NOT_DONE
    >= 0 when DONE
     */
    int chosenVariantIndex();
}
