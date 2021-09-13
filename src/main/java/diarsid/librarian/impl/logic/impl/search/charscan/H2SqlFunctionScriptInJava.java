package diarsid.librarian.impl.logic.impl.search.charscan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import diarsid.support.strings.MultilineMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;
import static java.time.LocalDateTime.now;

public interface H2SqlFunctionScriptInJava {

    String name();

    int version();

    default String nameAndVersion() {
        return this.name() + "_V" + this.version();
    }

    default Path sourceFile() {
        String relativeClassPath = this.getClass().getCanonicalName().replace('.', '/') + ".java";

        Path absoluteClassPath = Paths
                .get(".")
                .toAbsolutePath()
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve(relativeClassPath)
                .normalize();

        return absoluteClassPath;
    }

    default Path scriptFile() {
        Path absoluteScriptPath = Paths
                .get(".")
                .toAbsolutePath()
                .resolve("src")
                .resolve("main")
                .resolve("resources")
                .resolve("sql")
                .resolve("h2")
                .resolve(format("CREATE_FUNCTION_%s.sql", this.name()))
                .normalize();

        return absoluteScriptPath;
    }

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

        scriptLines.add(0, "-- generated ");
        scriptLines.add(1, "--   by " + this.getClass().getCanonicalName());
        scriptLines.add(2, "--   at " + now());
        scriptLines.add(3, format("CREATE ALIAS %s AS $$", this.nameAndVersion()));
        scriptLines.add(scriptLines.size(), "$$");

        return scriptLines;
    }

    default String scriptLinesJoined() throws Exception {
        return String.join(" \n", this.scriptLines());
    }

    default void rewriteScript() throws Exception {
        Logger log = LoggerFactory.getLogger(this.getClass());
        try {
            MultilineMessage message = new MultilineMessage("[script]", "   ");
            message.newLine().add("name: ").add(this.nameAndVersion());

            Path script = this.scriptFile();
            message.newLine().add("file: ").add(script.toString());

            if ( exists(script) ) {
                delete(script);
                message.newLine().add("removing existing file...");
            }

            write(script, this.scriptLines());
            message.newLine().add("written successfully.");
            log.info(message.compose());
        }
        catch (Exception e) {
            log.error("cannot rewrite script", e);
        }
    }
}
