package diarsid.librarian.tests.setup.transactional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class AwareOfTestAnnotations implements InvocationInterceptor {

    private static final ThreadLocal<List<Annotation>> ANNOTATIONS = new ThreadLocal<>();

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        ANNOTATIONS.set(asList(invocationContext.getExecutable().getAnnotations()));
        try {
            invocation.proceed();
        }
        finally {
            ANNOTATIONS.remove();
        }
    }

    public static <T> T oneAnnotationOrThrow(Class<T> type) {
        return ANNOTATIONS
                .get()
                .stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow();
    }

    public static <T> Optional<T> oneAnnotation(Class<T> type) {
        return ANNOTATIONS
                .get()
                .stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    public static <T> List<T> allAnnotations(Class<T> type) {
        return ANNOTATIONS
                .get()
                .stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(toList());
    }
}
