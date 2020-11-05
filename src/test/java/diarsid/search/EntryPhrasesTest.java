package diarsid.search;

public class EntryPhrasesTest {

//    @Test
//    public void fragmentsTest_phrases_in_phrase() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("MANY_WORDS_HERE", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.PHRASE));
//
//        List<PhraseInEntry> phrases = entry.phrases();
//        assertThat(phrases.size(), equalTo(1));
//
//        PhraseInEntry phrase = phrases.get(0);
//        assertThat(phrase.type(), equalTo(EntryFragment.Type.PHRASE));
//        assertThat(phrase.position(), equalTo(SINGLE));
//        assertThat(phrase.string(), equalTo("MANY_WORDS_HERE".toLowerCase()));
//    }
//
//    @Test
//    public void fragmentsTest_words_in_path() {
//        UUID userUuid = randomUUID();
//
//        RealEntry entry = new RealEntry("WORD/MANY_WORDS_HERE/word/_AND_HERE/ANDWORD", userUuid);
//        assertThat(entry.type(), equalTo(Entry.Type.PATH));
//
//        List<PhraseInEntry> phrases = entry.phrases();
//        assertThat(phrases.size(), equalTo(2));
//
//        PhraseInEntry phrase0 = phrases.get(0);
//        assertThat(phrase0.type(), equalTo(EntryFragment.Type.PHRASE));
//        assertThat(phrase0.position(), equalTo(FIRST));
//        assertThat(phrase0.index(), equalTo(0));
//        assertThat(phrase0.string(), equalTo("MANY_WORDS_HERE".toLowerCase()));
//
//        PhraseInEntry phrase1 = phrases.get(1);
//        assertThat(phrase1.type(), equalTo(EntryFragment.Type.PHRASE));
//        assertThat(phrase1.position(), equalTo(LAST));
//        assertThat(phrase1.index(), equalTo(1));
//        assertThat(phrase1.string(), equalTo("AND_HERE".toLowerCase()));
//    }
}
