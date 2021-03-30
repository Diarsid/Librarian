package diarsid.search;

public class EntryFragmentsTest {

//    @Test
//    public void fragmentsTest_word() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("SINGLEWORD", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.WORD));
//
//        List<EntryFragment> fragments = entry.fragments();
//        assertThat(fragments.size(), equalTo(1));
//
//        EntryFragment word = fragments.get(0);
//        assertThat(word.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word.position(), equalTo(SINGLE));
//        assertThat(word.string(), equalTo("SINGLEWORD".toLowerCase()));
//    }
//
//    @Test
//    public void fragmentsTest_phrase() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("MANY_WORDS_HERE", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.PHRASE));
//
//        List<EntryFragment> fragments = entry.fragments();
//        assertThat(fragments.size(), equalTo(1));
//
//        EntryFragment phrase = fragments.get(0);
//        assertThat(phrase.type(), equalTo(EntryFragment.Type.PHRASE));
//        assertThat(phrase.position(), equalTo(SINGLE));
//        assertThat(phrase.string(), equalTo("MANY_WORDS_HERE".toLowerCase()));
//    }
//
//    @Test
//    public void fragmentsTest_path_1() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("WORD/MANY_WORDS_HERE", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.PATH));
//
//        List<EntryFragment> fragments = entry.fragments();
//        assertThat(fragments.size(), equalTo(2));
//
//        EntryFragment word = fragments.get(0);
//        assertThat(word.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word.position(), equalTo(FIRST));
//        assertThat(word.string(), equalTo("WORD".toLowerCase()));
//
//        EntryFragment phrase = fragments.get(1);
//        assertThat(phrase.type(), equalTo(EntryFragment.Type.PHRASE));
//        assertThat(phrase.position(), equalTo(LAST));
//        assertThat(phrase.string(), equalTo("MANY_WORDS_HERE".toLowerCase()));
//    }
//
//    @Test
//    public void fragmentsTest_path_2() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("WORD/MANY_WORDS_HERE/_AND_HERE/ANDWORD", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.PATH));
//
//        List<EntryFragment> fragments = entry.fragments();
//        assertThat(fragments.size(), equalTo(4));
//
//        EntryFragment word = fragments.get(0);
//        assertThat(word.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word.position(), equalTo(FIRST));
//        assertThat(word.string(), equalTo("WORD".toLowerCase()));
//
//        EntryFragment phrase1 = fragments.get(1);
//        assertThat(phrase1.type(), equalTo(EntryFragment.Type.PHRASE));
//        assertThat(phrase1.position(), equalTo(MIDDLE));
//        assertThat(phrase1.string(), equalTo("MANY_WORDS_HERE".toLowerCase()));
//
//        EntryFragment phrase2 = fragments.get(2);
//        assertThat(phrase2.type(), equalTo(EntryFragment.Type.PHRASE));
//        assertThat(phrase2.position(), equalTo(MIDDLE));
//        assertThat(phrase2.string(), equalTo("_AND_HERE".toLowerCase()));
//
//        EntryFragment word2 = fragments.get(3);
//        assertThat(word2.type(), equalTo(EntryFragment.Type.WORD));
//        assertThat(word2.position(), equalTo(LAST));
//        assertThat(word2.string(), equalTo("ANDWORD".toLowerCase()));
//    }
}
