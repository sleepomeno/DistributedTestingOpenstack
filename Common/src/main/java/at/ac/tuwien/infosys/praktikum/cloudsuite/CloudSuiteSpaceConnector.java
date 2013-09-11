package at.ac.tuwien.infosys.praktikum.cloudsuite;

import at.ac.tuwien.infosys.praktikum.Util;
import at.ac.tuwien.infosys.praktikum.beans.ExecuteTestClass;
import at.ac.tuwien.infosys.praktikum.beans.ResourceRequest;
import at.ac.tuwien.infosys.praktikum.beans.ResourceResponse;
import at.ac.tuwien.infosys.praktikum.beans.TestResult;
import at.ac.tuwien.infosys.praktikum.space.ContainerManager;
import at.ac.tuwien.infosys.praktikum.space.SpaceCommunication;
import at.ac.tuwien.infosys.praktikum.space.SpaceCommunicationBuilder;
import at.ac.tuwien.infosys.praktikum.space.SpaceUser;
import com.bethecoder.ascii_table.ASCIITable;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mozartspaces.capi3.LabelCoordinator;
import org.mozartspaces.core.CapiUtil;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class CloudSuiteSpaceConnector {
    public static final ExecutorService executor = Executors.newCachedThreadPool();

    private static CloudSuiteSpaceConnector connector;
    private static Object lock = new Object();

    private final Logger log = LoggerFactory.getLogger(CloudSuiteSpaceConnector.class);
    private final SpaceUser spaceAgent = SpaceUser.createSpaceUserWithoutLocationLookup(Util.TESTER_PORT);

    private Map<String, Integer> numberOfTestclasses = new HashMap<>();
    private Map<String, Integer> numberOfReceivedIntermediateResults = new HashMap<>();

    private CloudSuiteSpaceConnector() { }

    /**
     * @return the Singleton instance
     */
    public static CloudSuiteSpaceConnector get() {
        synchronized (lock) {
           if(connector == null) {
               connector = new CloudSuiteSpaceConnector();
               try {
                   connector.start();
               } catch (MzsCoreException e) {
                   e.printStackTrace();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
           return connector;
        }
    }

    /**
     *  Sends test requests into the space
     */
    public void processTestClasses(List<Class<?>> testClasses, Class<?> suiteClass, String testId) {
        try {
            final ContainerReference masterTestsCont = ContainerManager.masterTests.getContainer();

            /* take the single test classes of suite and send requests for them to the master.
               attach a testId to make this request unique and a result can be fetched
             */
            for (Class<?> testClass : testClasses) {
                String name = testClass.getName();
                byte[] content = getBytes(Util.inClassPath(name));
                String suiteClassName = suiteClass.getName();
                List<String> testMethods = new ArrayList<>();

                Method[] declaredMethods = testClass.getDeclaredMethods();
                for (Method method : declaredMethods) {
                    boolean methodIsTestMethod = false;
                    Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
                    for (Annotation declaredAnnotation : declaredAnnotations) {
                        if (declaredAnnotation instanceof Test) {
                            methodIsTestMethod = true;
                        }
                    }
                    if (methodIsTestMethod) {
                        testMethods.add(method.getName());
                    }
                }

                ExecuteTestClass executeTestClass = new ExecuteTestClass(content, testId, suiteClassName, name, testMethods);

                log.info("Send a request to master: " + executeTestClass.toString());

                SpaceCommunication response = new SpaceCommunicationBuilder().setContainer(masterTestsCont)
                        .setItem(executeTestClass).build();
                spaceAgent.writeToSpace(response);

            }
            numberOfReceivedIntermediateResults.put(testId, 0);
            numberOfTestclasses.put(testId, testClasses.size());

        } catch (Exception e) {
            log.warn("Sending Test requests to Master failed!");
            e.printStackTrace();
        }
    }

    /**
     *  Initialize the connection to the space containers and wait for class and resource requests.
     */
    public void start() throws MzsCoreException, IOException {
        final ContainerReference classRequestsCont = ContainerManager.classRequests.getContainer();
        final ContainerReference classResponsesCont = ContainerManager.classResponses.getContainer();
        final ContainerReference resourceRequestsCont = ContainerManager.resourceRequests.getContainer();
        final ContainerReference resourceResponsesCont = ContainerManager.resourceResponses.getContainer();

        processResourceRequests(classRequestsCont, classResponsesCont);
        processResourceRequests(resourceRequestsCont, resourceResponsesCont);

    }

    private void processResourceRequests(final ContainerReference requestsCont, final ContainerReference responsesCont) {
        try {
            spaceAgent.notificationMgr.createNotification(requestsCont, new NotificationListener() {
                @Override
                public void entryOperationFinished(Notification notification, Operation operation, List<? extends Serializable> serializables) {
                   List<ResourceRequest> resourceRequests = Util.castEntries(serializables);

                    for (ResourceRequest resourceRequest : resourceRequests) {
                        SpaceCommunicationBuilder response = new SpaceCommunicationBuilder().setContainer(responsesCont);

                        String name = resourceRequest.identifier;
                        String inClasspath = resourceRequest.classPath;

                        log.debug(String.format("Try to find resource named %s", name));

                        // Load the requested resource from the classpath
                        byte[] bytes = getBytes(inClasspath);

                        if (bytes == null) {
                            log.warn("ERROR! Resource " + name + " could not be found! This case is not handled!");
                            return;
                        }
                        log.debug(String.format("Resource %s was found!", name));
                        response.setLabel(resourceRequest.label).setItem(new ResourceResponse(name, bytes));

                        // Send the requested resource into the space
                        spaceAgent.writeToSpace(response.build());
                    }
                }
            }, Operation.WRITE);
        } catch (MzsCoreException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private byte[] getBytes(String classPath) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(classPath);
        try {
            return IOUtils.toByteArray(is);
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void waitForResults(final String testId, final ReentrantLock reentrantLock) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                reentrantLock.lock();
                while (true) {
                    SpaceCommunication results = new SpaceCommunicationBuilder().setContainer(ContainerManager.clientResults.getContainer()).setSelector(LabelCoordinator.newSelector(testId, 1)).setTimeout(MzsConstants.RequestTimeout.INFINITE).build();
                    List<TestResult> testResults = spaceAgent.takeFromSpace(results);
                    TestResult testResult = CapiUtil.getSingleEntry(testResults);

                    synchronized (numberOfReceivedIntermediateResults) {

                        // we count the number of the intermediate results so that we know when the whole suite is finished
                        int number = numberOfReceivedIntermediateResults.get(testId);
                        numberOfReceivedIntermediateResults.put(testId, number + 1);
                    }

                    System.out.println("\n\nA test result for the test execution id " + testId + " for class " + testResult.testClassName + " has arrived:\n");
                    String[][] data = Util.getTestResultData(testResult);

                    String table = null;
                    try {
                        table = ASCIITable.getInstance().getTable(Util.getTestResultHeader(), data);
                    } catch (Exception e) {
                        log.warn("Exception while creating ASCIITable");
                        e.printStackTrace();
                    }
                    System.out.println(table + "\n\n");
                    if (numberOfTestclasses.get(testId) <= numberOfReceivedIntermediateResults.get(testId)) {
                        System.out.println("THE SUITE WITH ID " + testId + " HAS BEEN PROCESSED!\n\n");
                        reentrantLock.unlock();
                        return;
                    }
                }
            }
        }).start();
    }
}
