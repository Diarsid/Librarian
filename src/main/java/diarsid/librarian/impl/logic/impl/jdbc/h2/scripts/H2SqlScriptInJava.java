package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import diarsid.librarian.impl.logic.impl.jdbc.SqlScriptInJava;
import diarsid.support.strings.MultilineMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;
import static java.time.LocalDateTime.now;

public interface H2SqlScriptInJava extends SqlScriptInJava {

    List<String> scriptLines() throws Exception;

    default Path scriptFile() {
        Path absoluteScriptPath = Paths
                .get(".")
                .toAbsolutePath()
                .resolve("src")
                .resolve("main")
                .resolve("resources")
                .resolve("sql")
                .resolve("h2")
                .resolve(format("CREATE_%s_%s.sql",
                        this.scriptType().toUpperCase(),
                        this.name().toUpperCase()))
                .normalize();

        return absoluteScriptPath;
    }

    default String scriptLinesJoined() throws Exception {
        return String.join(" \n", this.markedScriptLines());
    }

    default List<String> markedScriptLines() throws Exception {
        List<String> scriptLines = this.scriptLines();

        scriptLines.add(0, "-- generated ");
        scriptLines.add(1, "--   by " + this.getClass().getCanonicalName());
        scriptLines.add(2, "--   at " + now());

        return scriptLines;
    }

    default void rewriteScript() {
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

            write(script, this.markedScriptLines());
            message.newLine().add("written successfully.");
            log.info(message.compose());
        }
        catch (Exception e) {
            log.error("cannot rewrite script", e);
        }
    }
}
