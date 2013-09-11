package at.ac.tuwien.infosys.praktikum.cloudsuite;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Custom Junit Suite
 * In {@link CloudSuite#CloudSuite(org.junit.runners.model.RunnerBuilder, Class, Class[])}
 * there is the entry point for the communication to the Cloud Space.
 */
public class CloudSuite extends Suite {
    private static CloudSuiteSpaceConnector spaceConnector = CloudSuiteSpaceConnector.get();

    public CloudSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        this(builder, klass, getAnnotatedClasses(klass));
    }

    public CloudSuite(RunnerBuilder builder, final Class<?> klass, Class<?>[] suiteClasses) throws InitializationError {
        super(klass, builder.runners(klass, suiteClasses));
        final List<Class<?>> classes = Arrays.asList(suiteClasses);
        // just a unique id
        String testId = UUID.randomUUID().toString();
        spaceConnector.processTestClasses(classes, klass, testId);

        ReentrantLock reentrantLock = new ReentrantLock();

        spaceConnector.waitForResults(testId, reentrantLock);

        try {
            // Wait a little bit so that the thread in waitForResults can acquire the lock
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        reentrantLock.lock();

    }

    public CloudSuite(Class<?> klass) throws InitializationError {
        this(klass, new AllDefaultPossibilitiesBuilder(true));
    }

    @Override
    protected void runChild(Runner runner, RunNotifier notifier) {
        // do nothing
    }

    // junit sublass internals
    private static Class<?>[] getAnnotatedClasses(Class<?> klass) throws InitializationError {
        SuiteClasses annotation = klass.getAnnotation(SuiteClasses.class);
        if (annotation == null)
            throw new InitializationError(String.format("class '%s' must have a SuiteClasses annotation", klass.getName()));
        return annotation.value();
    }
}
