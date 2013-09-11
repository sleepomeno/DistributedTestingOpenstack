package at.ac.tuwien.infosys.praktikum;

import at.ac.tuwien.infosys.praktikum.beans.BootComplete;
import at.ac.tuwien.infosys.praktikum.beans.HostExecuteTest;
import at.ac.tuwien.infosys.praktikum.beans.MethodResult;
import at.ac.tuwien.infosys.praktikum.beans.TestResult;
import at.ac.tuwien.infosys.praktikum.space.*;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.mozartspaces.capi3.LabelCoordinator;
import org.mozartspaces.core.CapiUtil;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Slave {
    public static final ExecutorService executor = Executors.newCachedThreadPool();

    private final Logger log = LoggerFactory.getLogger(Slave.class);
    private final SpaceUser spaceAgent = SpaceUser.createRemoteSpaceUser(9877);

    private String uuid;

    public static void main(String[] args) throws MzsCoreException, InterruptedException, UnknownHostException {
        Slave me = new Slave();
        me.start();
    }

    private void start() {
        this.uuid = Util.tryLookupIdFromNovaName().toString();
        log.debug("My uuid is " + uuid.toString());

        sendBootComplete();

        processTestRequest();
    }

    private void processTestRequest() {
        SpaceCommunication comm = new SpaceCommunicationBuilder().setContainer(ContainerManager.hostTests.getContainer())
                .setTimeout(MzsConstants.RequestTimeout.INFINITE)
                .setSelector(LabelCoordinator.newSelector(uuid, 1)).build();

        final List<HostExecuteTest> classes = spaceAgent.takeFromSpace(comm);
        final HostExecuteTest executeTest = CapiUtil.getSingleEntry(classes);

        log.info("Got a test request:");
        log.info(executeTest.toString());

        // Initialize ScriptManager correctly
        SpaceRemoteClassLoader classLoader = new SpaceRemoteClassLoader(spaceAgent, uuid, Slave.class.getClassLoader());
        ScriptManager.classLoader = classLoader;


        byte[] content = executeTest.content;
        String className = executeTest.name;
        List<String> testMethods = executeTest.testMethods;
        classLoader.addClass(className, content);


        try {
            Class clazz = Class.forName(className, false, classLoader);
            JUnitCore c = new JUnitCore();
            boolean wasSuccessful = true;
            List<MethodResult> methodResults = new ArrayList<>();
            for (String method : testMethods) {
                String identifier = clazz.getName() + "." + method;
                Result result = c.run(Request.method(clazz, method));

                long runTime = result.getRunTime();
                if (!result.wasSuccessful()) {
                    wasSuccessful = false;
                }

                List<Failure> failures = result.getFailures();
                String failure = failures.isEmpty() ? "" : failures.get(0).getMessage();

                MethodResult methodResult = new MethodResult(identifier, runTime, failure, result.wasSuccessful());
                methodResults.add(methodResult);
            }

            TestResult testResult = new TestResult(executeTest.testId, wasSuccessful, methodResults, uuid, className);

            SpaceCommunication writeComm = new SpaceCommunicationBuilder().setContainer(ContainerManager.testResults.getContainer())
                    .setLabel(executeTest.testId).setItem(testResult).build();
            spaceAgent.writeToSpace(writeComm);

            log.info("Sent a testresult to space: " + testResult.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        processTestRequest();
    }

    private void sendBootComplete() {
        BootComplete bootComplete = new BootComplete(uuid, new Date());
        SpaceCommunication writeComm = new SpaceCommunicationBuilder().setContainer(ContainerManager.bootComplete.getContainer())
                .setLabel(uuid).setItem(bootComplete).build();
        spaceAgent.writeToSpace(writeComm);

        log.info("Sent Boot complete!");
    }


}
