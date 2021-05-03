package diarsid.librarian.tests;

import java.util.List;

import diarsid.librarian.api.model.Entry;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class EntriesResult {

    final List<Entry> resultingEntries;

    public EntriesResult(List<Entry> resultingEntries) {
        this.resultingEntries = resultingEntries;
    }

    public int size() {
        return resultingEntries.size();
    }

    public boolean hasAny() {
        return resultingEntries.size() > 0;
    }

    public List<Entry> list() {
        return resultingEntries;
    }

    public void expectNoEntries() {
        assertThat(this.resultingEntries).isEmpty();
    }

    public void expectSomeEntries() {
        assertThat(this.resultingEntries.size()).isGreaterThan(0);
    }

    public void expectEntriesCount(int count) {
        assertThat(this.resultingEntries.size()).isEqualTo(count);
    }

    public void expectEntriesCountNoLessThan(int count) {
        assertThat(this.resultingEntries.size()).isGreaterThanOrEqualTo(count);
    }

    public void expectContainingAllStringsInEveryEntry(String... entries) {
        expectSomeEntries();
        List<String> strings = asList(entries);

        List<String> entriesNotContainingAllOfStrings = this.resultingEntries
                .stream()
                .map(Entry::string)
                .filter(entry -> {
                    String entryLower = entry.toLowerCase();
                    boolean entryNotOk = ! strings
                            .stream()
                            .allMatch(string -> {
                                boolean stringFound = entryLower.contains(string.toLowerCase());
                                return stringFound;
                            });
                    return entryNotOk;
                })
                .collect(toList());

        assertThat(entriesNotContainingAllOfStrings).isEmpty();
    }

    public void expectContainingAllStringsInMostOfEntries(String... entries) {
        expectSomeEntries();
        List<String> strings = asList(entries);

        List<String> entriesNotContainingAllOfStrings = this.resultingEntries
                .stream()
                .map(Entry::string)
                .filter(entry -> ! strings
                        .stream()
                        .allMatch(string -> entry.toLowerCase().contains(string.toLowerCase())))
                .collect(toList());

        if ( isNotEmpty(entriesNotContainingAllOfStrings) ) {
            assertThat(entriesNotContainingAllOfStrings).hasSizeLessThan(this.resultingEntries.size() / 3);
        }
    }

    public void expectContainingAnyString(String... entries) {
        expectSomeEntries();
        List<String> strings = asList(entries);

        List<String> stringsNotContainingAnyOfStrings = this.resultingEntries
                .stream()
                .map(Entry::string)
                .filter(entry -> strings
                        .stream()
                        .noneMatch(string -> entry.toLowerCase().contains(string.toLowerCase())))
                .collect(toList());

        assertThat(stringsNotContainingAnyOfStrings).isEmpty();
    }

    public void expectContainingString(String string) {
        String fragment = string.toLowerCase();
        expectEntriesCountNoLessThan(1);

        boolean contains = this.resultingEntries
                .stream()
                .map(Entry::string)
                .anyMatch(entry -> entry.toLowerCase().contains(fragment));

        if ( ! contains ) {
            fail();
        }
    }

    public void expectContainingStringInEveryEntry(String string) {
        String fragment = string.toLowerCase();
        expectEntriesCountNoLessThan(1);

        List<String> entriesNotContainingString = this.resultingEntries
                .stream()
                .map(Entry::string)
                .filter(entry -> ! entry.toLowerCase().contains(fragment))
                .collect(toList());

        assertThat(entriesNotContainingString).isEmpty();
    }

    public void expectOnlyEntries(String... entries) {
        expectEntriesCount(entries.length);
    }
}
