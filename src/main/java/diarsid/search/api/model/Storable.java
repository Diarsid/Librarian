package diarsid.search.api.model;

import diarsid.support.objects.CommonEnum;

import static diarsid.search.api.model.Storable.State.STORED;

public interface Storable {

    enum State implements CommonEnum<Storable.State> {
        STORED,
        NON_STORED
    }

    Storable.State state();

    Storable.State setState(Storable.State newState);

    default boolean hasState(Storable.State state) {
        return this.state().equals(state);
    }

    static void checkMustBeStored(Storable storable) {
        if ( storable.state().notEqualTo(STORED) ) {
            throw new IllegalArgumentException();
        }
    }

    static void checkMustBeStored(Iterable<? extends Storable> storables) {
        for ( Storable storable : storables ) {
            checkMustBeStored(storable);
        }
    }

    static void checkMustBeStored(Storable[] storables) {
        for ( Storable storable : storables ) {
            checkMustBeStored(storable);
        }
    }
}
