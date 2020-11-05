package diarsid.search.impl.logic.api.labels;

import java.time.LocalDateTime;

import diarsid.search.api.model.Entry;

public interface LabelsToChars {

    void join(Entry entry, Entry.Label label, LocalDateTime joiningTime);
}
