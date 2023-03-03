package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import diarsid.librarian.impl.logic.impl.search.charscan.NamedAndVersioned;

import static java.lang.String.format;

public abstract class H2SqlFunctionScriptInJava extends H2SqlScriptFileInJava {

    public static final String SCRIPT_MARKER = "/* script */";

    private final List<String> stringsToComment;
    private final List<String> stringsToRemove;

    public H2SqlFunctionScriptInJava(
            NamedAndVersioned source,
            List<String> stringsToComment,
            List<String> stringsToRemove) {
        super(source);
        this.stringsToComment = stringsToComment;
        this.stringsToRemove = stringsToRemove;
    }

    @Override
    public final String scriptType() {
        return "FUNCTION";
    }

    @Override
    public final List<String> scriptLines() throws Exception {
        List<String> allClassLines = Files.readAllLines(this.sourceFile());

        AtomicInteger scriptMarkerCounter = new AtomicInteger();
        List<String> scriptLines = new ArrayList<>();

        Consumer<String> collectJustScriptLines = (line) -> {
            if ( scriptMarkerCounter.get() == 0 ) {
                if ( line.contains(SCRIPT_MARKER) ) {
                    scriptMarkerCounter.incrementAndGet();
                }
            }
            else if ( scriptMarkerCounter.get() == 1 ) {
                if ( line.contains(SCRIPT_MARKER) ) {
                    scriptMarkerCounter.incrementAndGet();
                    return;
                }

                for ( String stringToRemove : this.stringsToRemove ) {
                    line = line.replace(stringToRemove, "");
                }

                for ( String stringToComment : this.stringsToComment ) {
                    line = line.replace(stringToComment, "//" + stringToComment);
                }

                scriptLines.add(line);
            }
        };

        allClassLines.forEach(collectJustScriptLines);

        if ( scriptMarkerCounter.get() != 2 ) {
            throw new IllegalStateException();
        }

        scriptLines.add(0, format("CREATE ALIAS %s AS $$", super.nameAndVersion()));
        scriptLines.add(scriptLines.size(), "$$");

        return scriptLines;
    }
}
