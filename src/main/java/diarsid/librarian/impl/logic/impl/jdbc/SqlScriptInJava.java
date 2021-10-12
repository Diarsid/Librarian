package diarsid.librarian.impl.logic.impl.jdbc;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface SqlScriptInJava {

    String scriptType();

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
}
