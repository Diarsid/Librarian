package diarsid.search;

public class EntryWordsTest {

//    @Test
//    public void fragmentsTest_word() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("SINGLEWORD", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.WORD));
//
//        List<WordInEntry> words = entry.words();
//        assertThat(words.size(), equalTo(1));
//
//        WordInEntry word = words.get(0);
//        assertThat(word.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word.position(), equalTo(SINGLE));
//        assertThat(word.string(), equalTo("SINGLEWORD".toLowerCase()));
//    }
//
//    @Test
//    public void fragmentsTest_words_in_path() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("WORD/MANY_WORDS_HERE/_AND_HERE/ANDWORD", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.PATH));
//
//        List<WordInEntry> words = entry.words();
//        assertThat(words.size(), equalTo(7));
//
//        WordInEntry word0 = words.get(0);
//        assertThat(word0.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word0.position(), equalTo(FIRST));
//        assertThat(word0.string(), equalTo("WORD".toLowerCase()));
//
//        assertThat(words.get(1).string(), equalTo("MANY".toLowerCase()));
//        assertThat(words.get(1).index(), equalTo(1));
//
//        assertThat(words.get(2).string(), equalTo("WORDS".toLowerCase()));
//        assertThat(words.get(2).index(), equalTo(2));
//
//        assertThat(words.get(3).string(), equalTo("HERE".toLowerCase()));
//        assertThat(words.get(3).index(), equalTo(3));
//
//        assertThat(words.get(4).string(), equalTo("AND".toLowerCase()));
//        assertThat(words.get(4).index(), equalTo(4));
//
//        assertThat(words.get(5).string(), equalTo("HERE".toLowerCase()));
//        assertThat(words.get(5).index(), equalTo(5));
//
//        WordInEntry word6 = words.get(6);
//        assertThat(word6.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word6.position(), equalTo(LAST));
//        assertThat(word6.string(), equalTo("ANDWORD".toLowerCase()));
//    }
//
//    @Test
//    public void fragmentsTest_words_in_phrase() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("MANY_WORDS_HERE", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.PHRASE));
//
//        List<WordInEntry> words = entry.words();
//        assertThat(words.size(), equalTo(3));
//
//        WordInEntry word0 = words.get(0);
//        assertThat(word0.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word0.position(), equalTo(FIRST));
//        assertThat(word0.string(), equalTo("MANY".toLowerCase()));
//
//        assertThat(words.get(1).string(), equalTo("WORDS".toLowerCase()));
//        assertThat(words.get(1).index(), equalTo(1));
//        assertThat(words.get(1).position(), equalTo(MIDDLE));
//
//        WordInEntry word6 = words.get(2);
//        assertThat(word6.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word6.position(), equalTo(LAST));
//        assertThat(word6.string(), equalTo("HERE".toLowerCase()));
//    }
}
