package diarsid.search.api.interaction;

import diarsid.support.objects.CommonEnum;

public interface UserChoice {

    enum Result implements CommonEnum<UserChoice.Result> {
        DONE,
        NOT_DONE,
        REJECTION
    }

    UserChoice.Result result();

    /*
    < 0 when this.type() returns REJECTION or NOT_DONE
    >= 0 when DONE
     */
    int chosenVariantIndex();
}
