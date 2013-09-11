package at.ac.tuwien.infosys.praktikum;

import at.ac.tuwien.infosys.praktikum.beans.*;
import at.ac.tuwien.infosys.praktikum.space.ContainerManager;
import at.ac.tuwien.infosys.praktikum.space.SpaceCommunication;
import at.ac.tuwien.infosys.praktikum.space.SpaceCommunicationBuilder;
import at.ac.tuwien.infosys.praktikum.space.SpaceUser;
import com.bethecoder.ascii_table.ASCIITable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.internal.util.Base64;
import org.mozartspaces.capi3.AnyCoordinator;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.LifoCoordinator;
import org.mozartspaces.core.CapiUtil;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;
import org.openstack.keystone.KeystoneClient;
import org.openstack.keystone.api.Authenticate;
import org.openstack.keystone.model.Access;
import org.openstack.keystone.model.Authentication;
import org.openstack.keystone.utils.KeystoneUtils;
import org.openstack.nova.NovaClient;
import org.openstack.nova.api.FlavorsCore;
import org.openstack.nova.api.ImagesCore;
import org.openstack.nova.api.ServersCore;
import org.openstack.nova.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Master {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final int DEFAULT_METHOD_DURATION = 2000;
    private static final int DEFAULT_BOOT_DURATION = 60000;
    private static final int SHUTDOWN_CHECK_PERIOD = 30000;
    private static final int BOOT_CHECK_PERIOD = 1000;
    private static final int SHUTDOWN_LIMIT = 60000;
    private static final int HOST_LIMIT = 7;

    private final Logger log = LoggerFactory.getLogger(Master.class);
    private final SpaceUser spaceAgent = SpaceUser.createSpaceUserWithoutLocationLookup(9875);


    private Map<String, HostState> hosts = Collections.synchronizedMap(new HashMap<String, HostState>());
    private Map<String, TestSuite> suites = Collections.synchronizedMap(new HashMap<String, TestSuite>());
    private Map<String, Long> methodRuntimes = Collections.synchronizedMap(new HashMap<String, Long>());
    private OpenStackWrapper openStack;
    private StatisticsUtil statistics = new StatisticsUtil();
    private AtomicLong lastBootDuration = new AtomicLong(DEFAULT_BOOT_DURATION);
    private AtomicInteger numberOfHostsRunning = new AtomicInteger(0);

    public static void main(String[] args) throws MzsCoreException, IOException {
        Master me = new Master();
        me.start();
    }

    private void start() throws MzsCoreException, IOException {
        final ContainerReference testResultsCont = ContainerManager.testResults.getContainer();

        this.openStack = new OpenStackWrapper();

        manageShutdowns();

        listenToSystemIn();

        processTestRequests();

        processHostBootComplete();

        processTestResults(testResultsCont);
    }

    private void manageShutdowns() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        synchronized (hosts) {
                            Collection<HostState> values = hosts.values();
                            for (HostState hostState : values) {
                                boolean isStillWorking = false;
                                Collection<TestRequest> requests = hostState.getTestRequests().values();
                                for (TestRequest request : requests) {
                                    if (request.getResult() == null) {
                                        isStillWorking = true;
                                    }
                                }
                                if (!isStillWorking && numberOfHostsRunning.get() > 1) {
                                    Date now = new Date();
                                    Date lastRequest = hostState.getLastAction();
                                    String uuid = hostState.getUuid();
                                    if (now.getTime() - SHUTDOWN_LIMIT > lastRequest.getTime()) {
                                        log.info("Let's shutdown host " + uuid);
                                        openStack.shutdownHost(uuid);
                                    } else {
                                        log.debug("Don't shutdown host " + uuid);
                                    }
                                }
                            }
                        }

                        Thread.sleep(SHUTDOWN_CHECK_PERIOD);

                    } catch (Exception e) {
                        log.warn("exception in manageShutdowns");
                        e.printStackTrace();
                    }
                }


            }
        });
    }

    private void listenToSystemIn() {
        final String SUITES_COMMAND = "suites";
        final String HOSTS_COMMAND = "hosts";

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);

                while (scanner.hasNextLine()) {
                    String input = scanner.next();
                    if (input.equalsIgnoreCase(SUITES_COMMAND)) {
                        String suiteStatistics = statistics.createSuiteStatistics();
                        writeToFile(suiteStatistics, "suites");
                    } else if (input.equalsIgnoreCase(HOSTS_COMMAND)) {
                        String hostStatistics = statistics.createHostStatistics();
                        writeToFile(hostStatistics, "hosts");
                    } else {
                        System.out.printf("Unknown command. Type \"%s\" to get the suites information or \"%s\" to get the hosts information%n", SUITES_COMMAND, HOSTS_COMMAND);
                    }
                }
            }
        });
    }

    private void processTestRequests() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ContainerReference masterTestsCont = ContainerManager.masterTests.getContainer();
                    SpaceCommunication comm = new SpaceCommunicationBuilder().setContainer(masterTestsCont)
                            .setSelector(FifoCoordinator.newSelector(1)).setTimeout(MzsConstants.RequestTimeout.INFINITE).build();

                    List<ExecuteTestClass> requests = spaceAgent.takeFromSpace(comm);
                    ExecuteTestClass request = CapiUtil.getSingleEntry(requests);

                    log.info("Got this request: " + request.toString());

                    addTestRequestToSuite(request);

                    processTestRequest(request.name, request.content, request.methods, request.testId, request.suiteClassName);
                }
            }
        });
    }

    private void processTestResults(final ContainerReference testResultsCont) {
        SpaceCommunication comm = new SpaceCommunicationBuilder().setContainer(testResultsCont)
                .setSelector(AnyCoordinator.newSelector(1)).setTimeout(MzsConstants.RequestTimeout.INFINITE).build();

        List<TestResult> results = spaceAgent.takeFromSpace(comm);
        final TestResult testResult = CapiUtil.getSingleEntry(results);

        putMethodRuntimesIntoCache(testResult);

        executor.execute(new Runnable() {
            @Override
            public void run() {


                log.info("Got a test result:");
                log.info(testResult.toString());

                String hostName = testResult.hostName;

                updateHostWithTestResult(hostName, testResult);

                informClientAboutTestResult(testResult);

                updateSuiteWithTestResult(testResult);

            }
        });

        processTestResults(testResultsCont);
    }

    private void addTestRequestToSuite(ExecuteTestClass request) {
        synchronized (suites) {
            if (!suites.containsKey(request.testId)) {
                TestSuite testSuite = new TestSuite(request.suiteClassName, request.testId);
                suites.put(request.testId, testSuite);
            }
            TestSuite testSuite = suites.get(request.testId);
            testSuite.addTestRequest(new TestRequest(request.name, request.methods, request.testId));
        }
    }

    private void writeToFile(String statistics, String fileName) {
//        System.out.println(statistics);
        try {
            FileUtils.writeStringToFile(new File(fileName), statistics);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<HostState> getReadyHosts() {
        List<HostState> hosts = new ArrayList<>();
        for (HostState state : this.hosts.values()) {
            if (state.getState() == HostState.State.READY) {
                hosts.add(state);
            }
        }
        return hosts;
    }

    private List<HostState> getWorkingHosts() {
        List<HostState> hosts = new ArrayList<>();
        for (HostState state : this.hosts.values()) {
            if (state.getState() == HostState.State.WORKING) {
                hosts.add(state);
            }
        }
        // sort the hosts considering the expectedFinishedDate
        Collections.sort(hosts, new Comparator<HostState>() {
            @Override
            public int compare(HostState o1, HostState o2) {
                Date finishedDate1 = o1.getExpectedFinishedDate();
                Date finishedDate2 = o2.getExpectedFinishedDate();
                if (finishedDate1 == null || finishedDate2 == null) {
                    throw new RuntimeException("Finished Date is null. Should never happen");
                } else {
                    return finishedDate1.compareTo(finishedDate2);
                }
            }
        });
        return hosts;
    }

    private List<HostState> getBootingHosts() {
        List<HostState> hosts = new ArrayList<>();
        for (HostState state : this.hosts.values()) {
            if (state.getBootDate() == null) {
                hosts.add(state);
            }
        }
        return hosts;
    }

    private void processTestRequest(final String className, final byte[] content, final List<String> methods, final String testId, String suiteClass) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (hosts) {
                        HostState chosenHost = null;
                        List<HostState> readyHosts = getReadyHosts();
                        if (!readyHosts.isEmpty()) {
                            chosenHost = readyHosts.get(0);
                            log.debug("Take a ready host!");
                        }
                        List<HostState> workingHosts = getWorkingHosts();
                        if (!workingHosts.isEmpty()) {
                            chosenHost = workingHosts.get(0);
                            log.debug("Take a working host!");
                        }

                        boolean forceNewHost = forceNewHost(chosenHost);

                        List<HostState> bootingHosts = getBootingHosts();
                        boolean bootIsInProgress = !bootingHosts.isEmpty();
                        boolean hostLimitNotReached = numberOfHostsRunning.get() <= HOST_LIMIT;
                        boolean noHostFound = chosenHost == null;
                        log.debug("Boot is in progress: " + bootIsInProgress);
                        log.debug("No host found: " + noHostFound);


                        // Here the policy when a new host is started is fixed!
                        if ((noHostFound && !bootIsInProgress && hostLimitNotReached) ||
                                (!noHostFound && !bootIsInProgress && hostLimitNotReached && forceNewHost)) {

                            openStack.startNewHost();
                            log.debug("Start a new host!");
                        } else {
                            if (bootIsInProgress) {
                                chosenHost = bootingHosts.get(0);
                            }
                            log.debug("Update the host with the test request");

                            updateHostWithTestRequest(chosenHost, testId, className, methods, content);
                            return;
                        }
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        log.warn("Unexpected exception in processTestRequest!");
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    private boolean forceNewHost(HostState chosenHost) {
        if (chosenHost == null) {
            return false;
        }
        Date expectedFinishedDate = chosenHost.getExpectedFinishedDate();
        boolean forceNewHostStart = false;
        if (expectedFinishedDate != null) {
            long durationOfLastBoot = lastBootDuration.get();
            long oldHostIsReady = expectedFinishedDate.getTime();
            log.debug("The old host is ready at: " + new Date(oldHostIsReady));
            Date now = new Date();
            log.debug("Now is " + now);
            long newHostIsReady = now.getTime() + durationOfLastBoot;
            log.debug("The new host would be ready at: " + new Date(newHostIsReady));
            if (newHostIsReady < oldHostIsReady) {
                forceNewHostStart = true;
                log.debug("Force a new host!");
            } else {
                log.debug("Don't force a new host!");
            }
        }
        return forceNewHostStart;
    }

    private void updateHostWithTestRequest(HostState chosenHost, String testId, String className, List<String> methods, byte[] content) {
        chosenHost.addTestRequest(testId + className, new TestRequest(className, methods, testId));
        Date newExpectedFinishedDate = computeExpectedFinishedDate(chosenHost, className, methods);
        chosenHost.setExpectedFinishedDate(newExpectedFinishedDate);
        chosenHost.setLastAction(new Date());

        String uuid = chosenHost.getUuid();
        log.debug("Host " + uuid + " is expected to be finished at " + newExpectedFinishedDate.toString());

        chosenHost.setState(HostState.State.WORKING);
        synchronized (suites) {
            suites.get(testId).setState(TestSuite.State.RUNNING);
        }
        log.info("Now I send the request for test class " + className + " to " + uuid + "!");
        sendTestToSlave(ContainerManager.hostTests.getContainer(), className, content, uuid, methods, testId);
        return;
    }

    private Date computeExpectedFinishedDate(HostState hostState, String className, List<String> methods) {
        long expectedDuration = computeExpectedDuration(className, methods);
        Date expectedFinishedDate = hostState.getExpectedFinishedDate();
        if (expectedFinishedDate == null) {
            if (hostState.getBootDate() == null) {
                expectedFinishedDate = new Date(new Date().getTime() + lastBootDuration.get());
            } else expectedFinishedDate = new Date();
        }
        return new Date(expectedFinishedDate.getTime() + expectedDuration);
    }

    private long computeExpectedDuration(String className, List<String> methods) {
        long expectedDuration = 0;
        synchronized (methodRuntimes) {
            for (String method : methods) {
                Long duration = methodRuntimes.get(className + "." + method);
                if (duration == null) {
                    expectedDuration = expectedDuration + DEFAULT_METHOD_DURATION;
                } else {
                    expectedDuration = expectedDuration + duration.longValue();
                    log.debug("Have found a cached value for method " + method + ": " + duration.longValue() + " ms!");
                }
            }
        }
        return expectedDuration;
    }

    private void updateSuiteWithTestResult(TestResult testResult) {
        synchronized (suites) {
            TestSuite testSuite = suites.get(testResult.label);
            TestRequest testRequest = testSuite.getTestByClassname(testResult.testClassName);
            testRequest.setResult(testResult);

            if (testSuite.allTestsDone()) {
                testSuite.setState(TestSuite.State.FINISHED);
            }
        }
    }

    private void putMethodRuntimesIntoCache(TestResult testResult) {
        synchronized (methodRuntimes) {
            List<MethodResult> methodResults = testResult.methodResults;
            for (MethodResult methodResult : methodResults) {
                methodRuntimes.put(methodResult.method, methodResult.runTime);
            }
        }
    }

    private void processHostBootComplete() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SpaceCommunication writeComm = new SpaceCommunicationBuilder().setSelector(LifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_MAX)).setContainer(ContainerManager.bootComplete.getContainer()).build();

                List<BootComplete> bootCompletes = spaceAgent.takeFromSpace(writeComm);
                for (BootComplete bootComplete : bootCompletes) {
                    String label = bootComplete.label;
                    synchronized (hosts) {
                        log.info("UUID of booted client is " + label);
                        if (!hosts.containsKey(label)) {
                            throw new RuntimeException("Should never happen! Master should already have this UUID!");
                        } else {
                            log.info(String.format("%s has finished booting!", label));
                            HostState hostState = hosts.get(label);
                            if(hostState.getState() != HostState.State.WORKING) {
                                hostState.setState(HostState.State.READY);
                            }
                            hostState.setBootDate(bootComplete.date);
                            hostState.setLastAction(new Date());
                            Date now = new Date();
                            Date bootIssued = hostState.getBootIssuedDate();
                            long newbootDuration = now.getTime() - bootIssued.getTime();
                            lastBootDuration.set(newbootDuration);
                            log.info("The new lastBootDuration is " + newbootDuration + " ms");
                        }
                    }
                }

            }
        }, 0, BOOT_CHECK_PERIOD);
    }

    private void sendTestToSlave(ContainerReference tests, String className, byte[] content, String uuid, List<String> methods, String testId) {
        HostExecuteTest executeTest = new HostExecuteTest(content, className, methods, testId);
        spaceAgent.writeToSpace(new SpaceCommunicationBuilder().setContainer(tests).setLabel(uuid).setItem(executeTest).build());

        log.info(String.format("Sent test for class %s and id %s to slave %s", className, testId, uuid));
    }

    private void updateHostWithTestResult(String hostName, TestResult testResult) {
        synchronized (hosts) {
            HostState hostState = hosts.get(hostName);
            String uuid = hostState.getUuid();
            log.debug("update host " + uuid + " with testresult " + testResult.label);
            Map<String, TestRequest> testRequests = hostState.getTestRequests();
            TestRequest testRequest = testRequests.get(testResult.getKey());
            testRequest.setResult(testResult);
            hostState.setLastAction(new Date());

            boolean isStillWorking = false;
            Collection<TestRequest> requests = testRequests.values();
            for (TestRequest request : requests) {
                if (request.getResult() == null) {
                    isStillWorking = true;
                }
            }
            if (!isStillWorking) {
                log.debug("Host " + hostName + " is not working any more!");
                hostState.setState(HostState.State.READY);
                hostState.setExpectedFinishedDate(null);
            } else {
                log.debug("host " + uuid + " is still working!");
            }
        }
    }


    private void informClientAboutTestResult(TestResult testResult) {
        SpaceCommunication informClient = new SpaceCommunicationBuilder().setContainer(ContainerManager.clientResults.getContainer())
                .setLabel(testResult.label).setItem(testResult).build();
        spaceAgent.writeToSpace(informClient);
    }

    private class StatisticsUtil {
        private String createHostStatistics() {
            StringBuilder builder = new StringBuilder();
            builder.append("===========\n");
            builder.append("HOST STATS\n");
            builder.append("===========\n\n");

            synchronized (hosts) {
                Collection<HostState> hosts = Master.this.hosts.values();
                if (hosts.isEmpty()) {
                    builder.append("There haven't been any online hosts so far!");
                    return builder.toString();
                }
                builder.append(hosts.size() + " hosts have been used!\n\n");
                for (HostState host : hosts) {
                    builder.append("########################################################\n");
                    builder.append("Host-Name: ").append(host.getUuid()).append("\n");
                    builder.append("Uptime: ").append(host.getTotalUptime()).append("\n");
                    builder.append("Number of tests: ").append(host.getNumberOfTestsRunOnHost()).append("\n");
                    builder.append("State: ").append(host.getState()).append("\n\n");
                    Map<String, TestRequest> testRequests = host.getTestRequests();
                    Collection<TestRequest> requests = testRequests.values();
                    if (requests.isEmpty()) {
                        builder.append("There have been no test requests so far!");
                        return builder.toString();
                    }
                    String[][] testResultData = null;
                    for (TestRequest request : requests) {
                        TestResult result = request.getResult();
                        if (result != null) {
                            String[][] resultData = Util.getTestResultData(result);
                            if (testResultData == null) {
                                testResultData = resultData;
                            } else {
                                testResultData = (String[][]) ArrayUtils.addAll(testResultData, resultData);
                            }
                        }
                    }

                    if (testResultData != null) {
                        String table = null;
                        try {
                            table = ASCIITable.getInstance().getTable(Util.getTestResultHeader(), testResultData);

                            builder.append(table);
                        } catch (Exception e) {
                            log.warn("Exception while creating the ASCIITable");
                            e.printStackTrace();
                        }
                    }
                    builder.append("\n");
                    builder.append("########################################################\n");
                    builder.append("\n\n");
                }
            }

            return builder.toString();

        }

        private String createSuiteStatistics() {
            StringBuilder builder = new StringBuilder();
            builder.append("===========\n");
            builder.append("SUITE STATS\n");
            builder.append("===========\n\n");

            synchronized (suites) {
                Collection<TestSuite> suites = Master.this.suites.values();
                if (suites.isEmpty()) {
                    builder.append("There have been no test suite requests so far!");
                    return builder.toString();
                }
                builder.append(suites.size() + " suite requests have been issued!\n\n");
                for (TestSuite suite : suites) {
                    builder.append("########################################################\n");
                    builder.append("Suite-Class: ").append(suite.suiteClassName).append("\n");
                    builder.append("Test-Id: ").append(suite.testId).append("\n");
                    builder.append("State: ").append(suite.getState()).append("\n");
                    builder.append("Hosts: ").append(StringUtils.join(suite.getHosts(), ',')).append("\n");

                    for (TestRequest request : suite.testRequests) {
                        TestResult result = request.getResult();
                        if (result != null) {
                            String[][] testResultData = Util.getTestResultData(result);
                            String table = null;
                            try {
                                table = ASCIITable.getInstance().getTable(Util.getTestResultHeader(), testResultData);
                                builder.append("\n\n");
                                builder.append("Test method results of ").append(result.testClassName).append("\n\n");

                                builder.append(table);
                            } catch (Exception e) {
                                log.warn("Exception while creating the ASCIITable");
                                e.printStackTrace();
                            }
                        }
                        builder.append("\n");
                    }
                    builder.append("########################################################\n");
                    builder.append("\n\n");

                }
            }

            return builder.toString();
        }
    }

    private class OpenStackWrapper {
        public static final String OPENSTACK_PROPERTIES = "openstack.properties";
        private NovaClient novaClient = createNovaClient();

        // Taken and adapted from CloudScale source
        private UUID startNewHost() {
            Properties openstackProps = Util.getPropertiesFromClasspath(OPENSTACK_PROPERTIES);
            UUID newId = UUID.randomUUID();
            ServerForCreate sfc = new ServerForCreate();
            String clientId = newId.toString();
            String clientUUID = Util.UUIDtoOpenstackId(clientId);
            sfc.setName(clientUUID);
            String flavorRef = lookupFlavorId(openstackProps.getProperty("flavor"));
            sfc.setFlavorRef(flavorRef);
            String slave = lookupImageId(openstackProps.getProperty("image"));
            sfc.setImageRef(slave);

            sfc.setKeyName("");

            // setting user data for server to discover space
            String spaceLocationData = Util.getLocalIP() + ":" + Util.MASTER_PORT;

            String IPFileOnServer = Util.IP_FILE;
            String IPFolderOnServer = new File(IPFileOnServer).getParent().replace('\\', '/');

            String spaceAddressDistributionUserScript = "#!/bin/sh" + "\n" +
                    "mkdir -p %s" + "\n" +
                    "echo \"%s\" > %s";
            String userData = String.format(spaceAddressDistributionUserScript,
                    IPFolderOnServer,
                    spaceLocationData,
                    IPFileOnServer);

            sfc.setUserData(Base64.encodeAsString(userData));

            novaClient.execute(ServersCore.createServer(sfc));

            HostState newHost = new HostState(clientId);
            newHost.setBootIssuedDate(new Date());
            hosts.put(clientId, newHost);
            numberOfHostsRunning.incrementAndGet();
            log.info("Client id of new client is " + clientId);

            return newId;
        }

        private String lookupFlavorId(String flavorName) {
            Flavors flavors = novaClient.execute(FlavorsCore.listFlavors());

            for (Flavor flavor : flavors.getList()) {
                if (flavor.getName().equalsIgnoreCase(flavorName)) {
                    return flavor.getId();
                }
            }

            throw new RuntimeException(String.format("Flavor with name %s could not be found", flavorName));
        }

        private String findOpenStackId(String instanceName) {
            Servers servers = novaClient.execute(ServersCore.listServers());
            for (Server server : servers.getList()) {
                if (server.getName().equals(instanceName)) {
                    String id = server.getId();
                    log.info("found the openStackId for " + instanceName + ": " + id);
                    return id;
                }
            }

            return null;
        }


        // Taken and adapted from CloudScale source
        private String lookupImageId(String imgName) {
            Images images = novaClient.execute(ImagesCore.listImages());

            for (Image img : images.getList()) {
                if (img.getName().equals(imgName)) {
                    return img.getId();
                }
            }

            throw new RuntimeException(String.format("Image with name %s could not be found", imgName));
        }


        // Taken and adapted from CloudScale source
        private NovaClient createNovaClient() {

            Properties openstackProps = Util.getPropertiesFromClasspath(OPENSTACK_PROPERTIES);

            KeystoneClient keystoneclient = new KeystoneClient(openstackProps.getProperty("endpoint"));

            Authentication.PasswordCredentials passwordCredentials = new Authentication.PasswordCredentials();
            passwordCredentials.setUsername(openstackProps.getProperty("username"));
            passwordCredentials.setPassword(openstackProps.getProperty("password"));

            Authentication authentication = new Authentication();
            authentication.setTenantName(openstackProps.getProperty("tenantName"));
            authentication.setPasswordCredentials(passwordCredentials);

            Access access = keystoneclient.execute(new Authenticate(authentication));

            return new NovaClient(
                    KeystoneUtils.findEndpointURL(access.getServiceCatalog(), "compute", null, "public"),
                    access.getToken().getId());
        }

        // Taken and adapted from CloudScale source
        private void shutdownHost(final String id) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String openstackId = findOpenStackId(Util.UUIDtoOpenstackId(id));
                    try {
                        synchronized (hosts) {
                            HostState hostState = hosts.get(id);
                            hostState.setState(HostState.State.SHUTDOWN_ISSUED);
                        }

                        novaClient.execute(ServersCore.deleteServer(openstackId));

                        final int SLEEP_TIME = 100;
                        int timeout = 100;//10 sec should be enough.
                        do {
                            Thread.sleep(SLEEP_TIME);
                        }
                        while (timeout-- > 0 && isHostRunning(openstackId));

                        if (timeout <= 0) {
                            log.warn("While Shutting down host with OpenStack Id=" + openstackId + " waited for timeout and server is still active.");
                        } else {
                            shutdownComplete(id, openstackId);
                        }
                    } catch (Exception e) {
                        log.error("Exception while shutting down host " + openstackId + ": " + e.getMessage());
                        shutdownComplete(id, openstackId);
                    }
                }

            });
        }

        private void shutdownComplete(String id, String openstackId) {
            log.info("Host " + openstackId + " has shutdown in time!");
            synchronized (hosts) {
                HostState hostState = hosts.get(id);
                hostState.setState(HostState.State.DOWN);
                numberOfHostsRunning.decrementAndGet();
            }
        }

        // Taken and adapted from CloudScale source
        public boolean isHostRunning(String openstackId) {
            Server serverInfo = novaClient.execute(ServersCore.showServer(openstackId));

            if (serverInfo == null)
                return false;

            return serverInfo.getStatus().equalsIgnoreCase("active");
        }

    }

}
