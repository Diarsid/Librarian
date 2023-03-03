package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import diarsid.librarian.impl.logic.impl.jdbc.SqlScriptInJava;
import diarsid.librarian.impl.logic.impl.search.charscan.NamedAndVersioned;
import diarsid.support.model.versioning.Version;
import diarsid.support.strings.MultilineMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;
import static java.time.LocalDateTime.now;

public abstract class H2SqlScriptFileInJava extends SqlScriptInJava {

    private String fileName;

    public H2SqlScriptFileInJava(NamedAndVersioned source) {
        super(source);
        this.fileName = null;
    }

    public H2SqlScriptFileInJava(Object source, String name, Version version) {
        super(source, name, version);
        this.fileName = null;
    }

    public final void overrideNameAndVersionInFile(String fileName) {
        this.fileName = fileName;
    }

    public final void unsetOverrideNameAndVersionInFile() {
        this.fileName = null;
    }

    public abstract List<String> scriptLines() throws Exception;

    public final Path scriptFile() {
        String name;
        if ( this.fileName == null ) {
            name = this.nameAndVersion().toUpperCase();
        }
        else {
            name = this.fileName.toUpperCase();
        }

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
                        name))
                .normalize();

        return absoluteScriptPath;
    }

    public final String scriptLinesJoined() throws Exception {
        return String.join(" \n", this.markedScriptLines());
    }

    public final List<String> markedScriptLines() throws Exception {
        List<String> scriptLines = this.scriptLines();

        scriptLines.add(0, "-- generated ");
        scriptLines.add(1, "--   by " + this.getClass().getCanonicalName());
        scriptLines.add(2, "--   at " + now());

        return scriptLines;
    }

    public final void writeScriptIntoFile() {
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
