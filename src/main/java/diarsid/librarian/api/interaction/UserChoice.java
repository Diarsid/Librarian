package diarsid.librarian.api.interaction;

import diarsid.librarian.api.annotations.ImplementationRequired;
import diarsid.support.objects.CommonEnum;

@ImplementationRequired
public interface UserChoice {

    enum Decision implements CommonEnum<Decision> {

        DONE(true),
        NOT_DONE(false),
        REJECTION(false);

        Decision(boolean hasIndex) {
            this.hasIndex = hasIndex;
        }

        public final boolean hasIndex;
    }

    Decision decision();

    /*
    By contract, it must return index of a variant, chosen by user. Should return:
    < 0 when this.type() returns REJECTION or NOT_DONE
    >= 0 when DONE
     */
    int chosenVariantIndex();
}
