package diarsid.librarian.tests.console;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import diarsid.librarian.api.Search;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.support.strings.MultilineMessage;

public class LoggingSearchObserver implements Search.Observer {

    private final Comparator<PatternToEntry> comparator;

    public LoggingSearchObserver(Comparator<PatternToEntry> comparator) {
        this.comparator = comparator;
    }

    @Override
    public void patternFound(Pattern pattern) {
        System.out.println("[OBSERVER] pattern found : " + pattern.string());
    }

    @Override
    public void relationsFound(List<PatternToEntry> relations) {
        if ( relations.isEmpty() ) {
            return;
        }

        MultilineMessage message = new MultilineMessage("[OBSERVER] ");
        message.newLine().add("relations found");
        message.newLine().add("pattern : ").add(relations.get(0).patternString());
        relations
                .stream()
                .sorted(comparator)
                .map(relation -> relation.weight() + " : " + relation.entry().string())
                .forEach(s -> message.newLine().add(s));

        System.out.println(message.compose());
    }

    @Override
    public void entriesFound(List<Entry> entries) {
        if ( entries.isEmpty() ) {
            return;
        }

        MultilineMessage message = new MultilineMessage("[OBSERVER] ");
        message.newLine().add("entries found");
        entries
                .stream()
                .map(entry -> entry.string())
                .forEach(s -> message.newLine().add(s));

        System.out.println(message.compose());
    }

    @Override
    public void entriesAssessed(List<Entry> rejectedEntries, List<PatternToEntry> relations) {
        if ( rejectedEntries.isEmpty() ) {
            return;
        }

        MultilineMessage message = new MultilineMessage("[OBSERVER] ");
        if ( relations.isEmpty() ) {
            message.newLine().add("rejected entries :");
            rejectedEntries
                    .stream()
                    .map(entry -> entry.string())
                    .forEach(s -> message.newLine().add(s));
        } else {
            message.newLine().add("entries assessed");
            message.newLine().add("pattern : ").add(relations.get(0).patternString());
            List<Entry> assessedEntries = new ArrayList<>();
            relations
                    .stream()
                    .sorted(comparator)
                    .peek(relation -> assessedEntries.add(relation.entry()))
                    .map(relation -> relation.weight() + " : " + relation.entry().string())
                    .forEach(s -> message.newLine().add(s));

            List<Entry> entriesCopy = new ArrayList<>(rejectedEntries);
            entriesCopy.removeAll(assessedEntries);

            if ( entriesCopy.isEmpty() ) {
                System.out.println(message.compose());
                return;
            }

            message.newLine().add("rejected entries :");
            entriesCopy
                    .stream()
                    .map(entry -> entry.string())
                    .forEach(s -> message.newLine().add(s));
        }

        System.out.println(message.compose());
    }
}
