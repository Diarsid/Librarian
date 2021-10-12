package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.lang.String.format;

public interface H2SqlFunctionScriptInJava extends H2SqlScriptInJava {

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
                scriptLines.add(line.replace("public ", "").replace("logln(\"", "//logln(\"")); // comment logln invocation
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
