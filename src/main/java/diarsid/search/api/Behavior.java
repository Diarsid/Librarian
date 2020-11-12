package diarsid.search.api;

public interface Behavior {

    Behavior DEFAULT = new Behavior() {

        @Override
        public void set(Feature feature, boolean enabled) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean get(Feature feature) {
            switch ( feature ) {
                case DECOMPOSE_ENTRY_PATH: return true;
                default: throw new UnsupportedOperationException();
            }
        }
    };

    enum Feature {
        /*
        * Means that if path entry 'my/very/long/path' is being stored, then derived entries:
        * - my
        * - my/very/
        * - my/very/long
        * will be also stored as new entries if they not exist.
        *
        * Important note 1 - only original entry 'my/very/long/path' will be returned from
        * Entries.save() method
        *
        * Important note 2 - if original entry is being saved with provided Labels, all derived entries
        * will be also bound to provided Labels
        * */
        DECOMPOSE_ENTRY_PATH
    }

    void set(Feature feature, boolean enabled);

    default void enable(Feature feature) {
        this.set(feature, true);
    }

    default void disable(Feature feature) {
        this.set(feature, false);
    }

    boolean get(Feature feature);

    default boolean isEnabled(Feature feature) {
        return this.get(feature);
    }

    default boolean isDisabled(Feature feature) {
        return ! this.get(feature);
    }
}
