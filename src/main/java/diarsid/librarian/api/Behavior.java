package diarsid.librarian.api;

import diarsid.librarian.api.model.User;

public interface Behavior {

    enum Feature {

        /*
        * Means that if path entry 'my/very/long/path' is being stored, then derived entries:
        * - my
        * - my/very/
        * - my/very/long
        * is also stored as new entries if they not exist.
        *
        * Note 1 - only original entry 'my/very/long/path' will be returned from
        * Entries.save() method
        *
        * Note 2 - if original entry is being saved with provided Labels and
        * APPLY_PARENT_LABELS_TO_DERIVED_ENTRIES == true, all derived entries
        * will be also bound to provided Labels.
        *
        * Note 3 - it is possible to instruct labels if bind or not to particular derived entries via
        * Entry.Label.bindableIf((entry, label) -> {
        *     boolean ifLabelShouldBoundToEntry = // your logic
        *     return ifLabelShouldBoundToEntry;
        * });
        *
        * Example:
        * <code>
        *   Entry.Label tools = core.store().labels().getOrSave(user, "tools");
        *   Entry.Label servers = core.store().labels().getOrSave(user, "servers");
        *
        *   core.store().entries().addLabels(
        *         user, "My/Tools/Servers",
        *         tools.bindableIf((entry, label) -> entry.string().contains("tools")),
        *         servers.bindableIf((entry, label) -> entry.string().contains("servers"))
        *   );
        * </code>
        *
        * - entry "My/Tools/Servers" is bound to both "tools" and "servers" labels
        * - derived entry "My/Tools" is bound only to "tools" label
        * - derived entry "My" is not bound to any label at all
        * */
        DECOMPOSE_ENTRY_PATH( /* default value */ true),

        /*
        * See DECOMPOSE_ENTRY_PATH description.
        * */
        APPLY_PARENT_LABELS_TO_DERIVED_ENTRIES( /* default value */ true),

        /*
        * If set to true, word like CamelCaseWord is treated as 3 different words in sense of
        * analize. If set to false, it is treated as 1 long word.
        * */
        USE_CAMEL_CASE_WORDS_DECOMPOSITION( /* default value */ true),

        /*
        * If set to true, single chars in entry, separated by any separators, is joined to next word.
        *
        * For example, having entry "A Book Name, written by D.J.Doe"
        * if set to true, words are:
        * "ABook", "Name", "written", "by", "DJDoe"
        * is set to false, words are:
        * "A", "Book", "Name", "written", "by", "D", "J", "Doe"
        *
        * First is preferred as it simplifies search by patterns like "abookjdoe", which includes characters
        * from 2 words instead of 4.
        * */
        JOIN_SINGLE_CHARS_TO_NEXT_WORD( /* default value */ true);

        private final boolean defaultValue;

        Feature(boolean defaultValue) {
            this.defaultValue = defaultValue;
        }

        public boolean defaultValue() {
            return defaultValue;
        }
    }

    void set(User user, Feature feature, boolean enabled);

    default void enable(User user, Feature feature) {
        this.set(user, feature, true);
    }

    default void disable(User user, Feature feature) {
        this.set(user, feature, false);
    }

    boolean get(User user, Feature feature);

    default boolean isEnabled(User user, Feature feature) {
        return this.get(user, feature);
    }

    default boolean isDisabled(User user, Feature feature) {
        return ! this.get(user, feature);
    }
}
