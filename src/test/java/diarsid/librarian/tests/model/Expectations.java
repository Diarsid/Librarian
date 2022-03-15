package diarsid.librarian.tests.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import diarsid.librarian.api.model.Entry;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class Expectations {
    
    final List<Entry> entries;
    final List<Expectation> expectations;

    public Expectations(List<Entry> entries) {
        this.entries = entries;
        this.expectations = new ArrayList<>();
    }

    private void addNewExpectation(Expectation expectation) {
        if ( ! this.expectations.contains(expectation) ) {
            this.expectations.add(expectation);
        }
    }

    private void addNewExpectation(Consumer<List<Entry>> assertion) {
        this.expectations.add(new Expectation(assertion));
    }

    void reset() {
        this.expectations.clear();
    }

    public Expectations noEntries() {
        this.addNewExpectation(entries -> assertThat(entries).isEmpty());
        return this;
    }

    public Expectations someEntries() {
        this.addNewExpectation(entries -> assertThat(entries).isNotEmpty());
        return this;
    }

    public Expectations entriesCount(int count) {
        this.someEntries();
        this.addNewExpectation(entries -> assertThat(entries.size()).isEqualTo(count));
        return this;
    }

    public Expectations entriesCountNoLessThan(int count) {
        this.someEntries();
        this.addNewExpectation(entries -> assertThat(entries.size()).isGreaterThanOrEqualTo(count));
        return this;
    }

    public Expectations containingAllStringsInEveryEntry(String... entryStrings) {
        this.someEntries();
        this.addNewExpectation(entries -> {
            List<String> strings = asList(entryStrings);

            List<String> entriesNotContainingAllOfStrings = entries
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
        });
        return this;
    }

    public Expectations containingAllStringsInAtLeastOneEntry(String... entryStrings) {
        this.someEntries();
        this.addNewExpectation(entries -> {
            List<String> strings = asList(entryStrings);

            boolean contains = entries
                    .stream()
                    .map(Entry::string)
                    .anyMatch(entry -> {
                        String entryLower = entry.toLowerCase();
                        boolean entryOk = strings
                                .stream()
                                .allMatch(string -> {
                                    boolean stringFound = entryLower.contains(string.toLowerCase());
                                    return stringFound;
                                });
                        return entryOk;
                    });

            assertThat(contains).isTrue();
        });
        return this;
    }

    public Expectations containingAllStringsInMostOfEntries(String... entryStrings) {
        this.someEntries();
        this.addNewExpectation(entries -> {
            List<String> strings = asList(entryStrings);

            List<String> entriesNotContainingAllOfStrings = entries
                    .stream()
                    .map(Entry::string)
                    .filter(entry -> ! strings
                            .stream()
                            .allMatch(string -> entry.toLowerCase().contains(string.toLowerCase())))
                    .collect(toList());

            if ( isNotEmpty(entriesNotContainingAllOfStrings) ) {
                int minority = entries.size() / 4;
                if ( minority == 0 ) {
                    minority = 1;
                }
                assertThat(entriesNotContainingAllOfStrings).hasSizeLessThanOrEqualTo(minority);
            }
        });
        return this;
    }

    public Expectations containingAllStringsInMostOfEntries(List<String> strings, float rate /* [0.1 -- 0.9] */) {
        assertThat(rate).isBetween(0.1f, 0.9f);
        this.someEntries();
        this.addNewExpectation(entries -> {
            List<String> stringsLower = strings
                    .stream()
                    .map(s -> s.strip().trim().toLowerCase())
                    .collect(toList());

            List<String> entriesNotContainingAllOfStrings = entries
                    .stream()
                    .map(Entry::string)
                    .filter(entry -> {
                        String entryLower = entry.toLowerCase();
                        return ! stringsLower
                                .stream()
                                .allMatch(stringLower -> entryLower.contains(stringLower));
                    })
                    .collect(toList());

            if ( isNotEmpty(entriesNotContainingAllOfStrings) ) {
                int minority = (int) (entries.size() * rate);
                if ( minority == 0 ) {
                    minority = 1;
                }
                assertThat(entriesNotContainingAllOfStrings).hasSizeLessThanOrEqualTo(minority);
            }
        });
        return this;
    }

    public Expectations containingStringInMostOfEntries(String string) {
        this.someEntries();
        this.addNewExpectation(entries -> {
            String lowerString = string.strip().trim().toLowerCase();

            List<String> entriesNotContainingAllOfStrings = entries
                    .stream()
                    .map(Entry::string)
                    .filter(entry -> ! entry.toLowerCase().contains(lowerString))
                    .collect(toList());

            if ( isNotEmpty(entriesNotContainingAllOfStrings) ) {
                int minority = entries.size() / 3;
                if ( minority == 0 ) {
                    minority = 1;
                }
                assertThat(entriesNotContainingAllOfStrings).hasSizeLessThanOrEqualTo(minority);
            }
        });
        return this;
    }

    public Expectations containingStringInSignificantCountOfEntries(String string) {
        this.someEntries();
        this.addNewExpectation(entries -> {
            String lowerString = string.strip().trim().toLowerCase();

            List<String> entriesContainingAllOfStrings = entries
                    .stream()
                    .map(Entry::string)
                    .filter(entry -> entry.toLowerCase().contains(lowerString))
                    .collect(toList());

            int count = entriesContainingAllOfStrings.size();

            if ( count == 0 ) {
                fail();
            }
            else {
                int threshold;
                if ( entries.size() > 9 ) {
                    threshold = 3;
                }
                else {
                    threshold = 1;
                }

                if ( count < threshold ) {
                    fail();
                }
            }
        });
        return this;
    }

    public Expectations containingStringsInSignificantCountOfEntries(String... strings) {
        this.someEntries();
        this.addNewExpectation(entries -> {
            List<String> lowerStrings = stream(strings)
                    .map(string -> string.strip().trim().toLowerCase())
                    .collect(toList());

            List<String> entriesContainingAllOfStrings = entries
                    .stream()
                    .map(Entry::string)
                    .filter(entry -> {
                        String lowerEntry = entry.toLowerCase();
                        return lowerStrings
                                .stream()
                                .allMatch(lowerString -> lowerEntry.contains(lowerString));
                    })
                    .collect(toList());

            int count = entriesContainingAllOfStrings.size();

            if ( count == 0 ) {
                fail();
            }
            else {
                int threshold;
                if ( entries.size() > 9 ) {
                    threshold = 3;
                }
                else {
                    threshold = 1;
                }

                if ( count < threshold ) {
                    fail();
                }
            }
        });
        return this;
    }

    public Expectations containingStringInMostOfEntries(String string, float rate /* [0.1 -- 0.9] */) {
        assertThat(rate).isBetween(0.1f, 0.9f);
        this.someEntries();
        this.addNewExpectation(entries -> {
            String lowerString = string.strip().trim().toLowerCase();

            List<String> entriesNotContainingAllOfStrings = entries
                    .stream()
                    .map(Entry::string)
                    .filter(entry -> ! entry.toLowerCase().contains(lowerString))
                    .collect(toList());

            if ( isNotEmpty(entriesNotContainingAllOfStrings) ) {
                int minority = (int) (entries.size() * rate);
                if ( minority == 0 ) {
                    minority = 1;
                }
                assertThat(entriesNotContainingAllOfStrings).hasSizeLessThan(minority);
            }
        });
        return this;
    }

    public Expectations containingAnyString(String... entryStrings) {
        this.someEntries();
        this.addNewExpectation(entries -> {
            List<String> strings = asList(entryStrings);

            List<String> stringsNotContainingAnyOfStrings = entries
                    .stream()
                    .map(Entry::string)
                    .filter(entry -> strings
                            .stream()
                            .noneMatch(string -> entry.toLowerCase().contains(string.toLowerCase())))
                    .collect(toList());

            assertThat(stringsNotContainingAnyOfStrings).isEmpty();
        });
        return this;
    }

    public Expectations containingString(String string) {
        this.entriesCountNoLessThan(1);
        this.addNewExpectation(entries -> {
            String fragment = string.toLowerCase();
            boolean contains = entries
                    .stream()
                    .map(Entry::string)
                    .anyMatch(entry -> entry.toLowerCase().contains(fragment));

            if ( ! contains ) {
                fail();
            }
        });
        return this;
    }

    public Expectations notContainingString(String string) {
        this.addNewExpectation(entries -> {
            String fragment = string.toLowerCase();
            boolean contains = entries
                    .stream()
                    .map(Entry::string)
                    .anyMatch(entry -> entry.toLowerCase().contains(fragment));

            if ( contains ) {
                fail();
            }
        });
        return this;
    }

    public Expectations containingStringInEveryEntry(String string) {
        this.entriesCountNoLessThan(1);
        this.addNewExpectation(entries -> {
            String fragment = string.toLowerCase();
            List<String> entriesNotContainingString = entries
                    .stream()
                    .map(Entry::string)
                    .filter(entry -> ! entry.toLowerCase().contains(fragment))
                    .collect(toList());

            assertThat(entriesNotContainingString).isEmpty();
        });
        return this;
    }

    public Expectations onlyEntries(String... entries) {
        this.entriesCount(entries.length);
        return this;
    }

    public void andAssert() {
        String errors = this.expectations
                .stream()
                .peek(expectation -> expectation.test(this.entries))
                .filter(expectation -> expectation.isFailed())
                .map(expectation -> expectation.error())
                .map(error -> error.getMessage())
                .collect(joining(System.lineSeparator()));

        if ( errors.isEmpty() ) {
            return;
        }

        this.reset();

        throw new AssertionError(errors);
    }
}
