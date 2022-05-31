package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

public interface H2SqlFunctionScriptInJava extends H2SqlScriptInJava {

    default List<String> stringsToCommentInScript() {
        return emptyList();
    }

    default List<String> stringsToRemoveInScript() {
        return emptyList();
    }

    @Override
    default String scriptType() {
        return "FUNCTION";
    }

    @Override
    default List<String> scriptLines() throws Exception {
        List<String> allClassLines = Files.readAllLines(this.sourceFile());

        AtomicInteger scriptMarkerCounter = new AtomicInteger();
        List<String> scriptLines = new ArrayList<>();

        Consumer<String> aggregateClassLines = (line) -> {
            if ( scriptMarkerCounter.get() == 0 ) {
                if ( ! line.contains("/* script */") ) {
                    return;
                }
                else {
                    scriptMarkerCounter.incrementAndGet();
                }
            }
            else if ( scriptMarkerCounter.get() == 1 ) {
                if ( line.contains("/* script */") ) {
                    scriptMarkerCounter.incrementAndGet();
                    return;
                }

                for ( String stringToRemove : this.stringsToRemoveInScript() ) {
                    line = line.replace(stringToRemove, "");
                }

                for ( String stringToComment : this.stringsToCommentInScript() ) {
                    line = line.replace(stringToComment, "//"+stringToComment);
                }

                scriptLines.add(line);
            }
        };

        allClassLines.forEach(aggregateClassLines);

        if ( scriptMarkerCounter.get() != 2 ) {
            throw new IllegalStateException();
        }

        scriptLines.add(0, format("CREATE ALIAS %s AS $$", this.nameAndVersion()));
        scriptLines.add(scriptLines.size(), "$$");

        return scriptLines;
    }
}
