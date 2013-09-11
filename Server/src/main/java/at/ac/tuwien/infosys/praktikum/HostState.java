package at.ac.tuwien.infosys.praktikum;

import at.ac.tuwien.infosys.praktikum.beans.TestRequest;
import at.ac.tuwien.infosys.praktikum.beans.TestResult;

import java.util.*;

public class HostState {
    private final String uuid;

    // when the host gets its first test request its state
    // changes to WORKING (even if it hasn't fully booted yet)
    public enum State {
        STARTED, READY, WORKING, SHUTDOWN_ISSUED, DOWN;
    }

    private State state = State.STARTED;
    private Date bootDate;
    private Date lastAction;
    private Date bootIssuedDate;


    public HostState(String uuid) {
         this.uuid = uuid;
    }

    /**
     * Null if the host is already free, otherwise the expected date when
     * the host will have finished their scheduled hostTests
     */
    private Date expectedFinishedDate;

    private Map<String, TestRequest> testRequests = Collections.synchronizedMap(new HashMap<String, TestRequest>());

    public int getNumberOfTestsRunOnHost() {
        Collection<TestRequest> requests = testRequests.values();
        int number = 0;
        for (TestRequest request : requests) {
            TestResult result = request.getResult();
            if(result != null) {
                number = number + result.methodResults.size();
            }
        }

        return number;
    }

    public String getUuid() {
        return uuid;
    }

    public String getTotalUptime() {
        if(bootDate == null) {
            return "The host has not fully started yet!";
        }
        Date now = new Date();
        long difference = now.getTime() - bootDate.getTime();
        long diffInSeconds = difference / 1000;

        return String.valueOf(diffInSeconds) + " seconds";

    }

    public Date getLastAction() {
        return lastAction;
    }

    public void setLastAction(Date lastAction) {
        this.lastAction = lastAction;
    }

    public Date getBootIssuedDate() {
        return bootIssuedDate;
    }

    public void setBootIssuedDate(Date bootIssuedDate) {
        this.bootIssuedDate = bootIssuedDate;
    }

    public Date getBootDate() {
        return bootDate;
    }

    public Map<String, TestRequest> getTestRequests() {
        return testRequests;
    }

    public void addTestRequest(String key, TestRequest request) {
        testRequests.put(key, request);
        lastAction = new Date();
    }

    public void setBootDate(Date bootDate) {
        this.bootDate = bootDate;
    }

    public Date getExpectedFinishedDate() {
        return expectedFinishedDate;
    }

    public void setExpectedFinishedDate(Date expectedFinishedDate) {
        this.expectedFinishedDate = expectedFinishedDate;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
