package diarsid.librarian.impl.logic.impl.search.charscan;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.lang.String.format;

public abstract class LoggingInScript {

    private final AtomicBoolean enabled;
    private final Consumer<String> loggingDelegate;

    public LoggingInScript() {
        this.enabled = new AtomicBoolean(true);
        this.loggingDelegate = System.out::println;
    }

    public LoggingInScript(boolean enabled, Consumer<String> logDelegate) {
        this.enabled = new AtomicBoolean(enabled);
        this.loggingDelegate = logDelegate;
    }

    public boolean loggingEnabled() {
        return this.enabled.get();
    }

    public void setLoggingEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public void logln(String line) {
        if ( this.enabled.get() ) {
            this.loggingDelegate.accept(line);
        }
    }

    public void logln(String line, Object... args) {
        if ( this.enabled.get() ) {
            this.loggingDelegate.accept(format(line, args));
        }
    }
}
