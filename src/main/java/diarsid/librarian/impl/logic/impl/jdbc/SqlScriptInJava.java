package diarsid.librarian.impl.logic.impl.jdbc;

import java.nio.file.Path;
import java.nio.file.Paths;

import diarsid.librarian.impl.logic.impl.search.charscan.NamedAndVersioned;
import diarsid.support.model.versioning.Version;

public abstract class SqlScriptInJava implements NamedAndVersioned {

    private final Version version;
    private final String name;
    private final String nameAndVersion;
    protected final Object source;

    public SqlScriptInJava(NamedAndVersioned source) {
        this.source = source;
        this.version = source.version();
        this.name = source.name();
        this.nameAndVersion = source.nameAndVersion();
    }

    public SqlScriptInJava(Object source, String name, Version version) {
        this.source = source;
        this.name = name;
        this.version = version;
        this.nameAndVersion = NamedAndVersioned.nameAndVersionOf(name, version);
    }

    public abstract String scriptType();

    @Override
    public final String nameAndVersion() {
        return this.nameAndVersion;
    }

    @Override
    public final String name() {
        return this.name;
    }

    @Override
    public final Version version() {
        return this.version;
    }

    public final Path sourceFile() {
        String relativeClassPath = this.source.getClass().getCanonicalName().replace('.', '/') + ".java";

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
