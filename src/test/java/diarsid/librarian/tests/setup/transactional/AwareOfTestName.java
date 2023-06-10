package diarsid.librarian.tests.setup.transactional;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static java.util.stream.Collectors.toList;

import static diarsid.support.strings.StringUtils.splitByAnySeparators;
import static diarsid.support.strings.StringUtils.splitCamelCase;

public class AwareOfTestName implements InvocationInterceptor {

    private final static ThreadLocal<String> TEST_NAME = new ThreadLocal<>();
    private final static ThreadLocal<List<String>> TEST_NAME_PARTS = new ThreadLocal<>();

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        String testMethod = invocationContext.getExecutable().getName();

        TEST_NAME.set(testMethod);

        List<String> parts = splitByAnySeparators(testMethod)
                .stream()
                .flatMap(part -> splitCamelCase(part, false).stream())
                .collect(toList());

        TEST_NAME_PARTS.set(parts);

        try {
            invocation.proceed();
        }
        finally {
            TEST_NAME.remove();
            TEST_NAME_PARTS.remove();
        }
    }

    public static String testName() {
        return TEST_NAME.get();
    }

    public static List<String> testNameParts() {
        return TEST_NAME_PARTS.get();
    }
}
