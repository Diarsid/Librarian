package diarsid.librarian.impl.logic.impl.search.charscan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.time.LocalDateTime.now;

public class CopyMethodToEvalMatchingSrcipt {

    public static void main(String[] args) throws Exception {
        int version = PatternToWordMatching.CURRENT_VERSION.version();

        String relativeClassPath = PatternToWordMatching
                .CURRENT_VERSION
                .getClass()
                .getCanonicalName()
                .replace('.', '/')
                + ".java";

        Path absoluteClassPath = Paths
                .get(".")
                .toAbsolutePath()
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve(relativeClassPath)
                .normalize();

        List<String> allClassLines = Files.readAllLines(absoluteClassPath);
        List<String> methodLines = new ArrayList<>();

        AtomicInteger scriptMarkerCounter = new AtomicInteger();

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
                methodLines.add(line.replace("logln(\"", "//logln(\"")); // comment logln invocation
            }
        };

        allClassLines.forEach(aggregateClassLines);

        Path absoluteScriptPath = Paths
                .get(".")
                .toAbsolutePath()
                .resolve("src")
                .resolve("main")
                .resolve("resources")
                .resolve("CREATE_FUNCTION_EVAL_MATCHING.sql")
                .normalize();

        if ( Files.exists(absoluteScriptPath) ) {
            Files.delete(absoluteClassPath);
        }

        methodLines.add("-- generated at " + now());
        methodLines.add(0, "CREATE ALIAS EVAL_MATCHING_Vx AS $$".replace("x", String.valueOf(version)));
        methodLines.add(methodLines.size(), "$$");
        Files.write(absoluteScriptPath, methodLines);
    }

    public static void generateScriptFrom() {

    }
}
