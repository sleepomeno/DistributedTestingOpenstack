package at.ac.tuwien.infosys.praktikum.beans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestSuite {
    public enum State {
        SCHEDULED, RUNNING, FINISHED;
    }

    public final String suiteClassName;
    public final List<TestRequest> testRequests = new ArrayList<>();
    public final String testId;

    private State state = State.SCHEDULED;

    public TestSuite(String suiteClassName, String testId) {
        this.suiteClassName = suiteClassName;

        this.testId = testId;
    }

    public Set<String> getHosts() {
        Set<String> hosts = new HashSet<>();
        for (TestRequest testRequest : testRequests) {
            TestResult result = testRequest.getResult();
            if (result != null) {
                hosts.add(result.hostName);
            }
        }

        return hosts;
    }

    public void addTestRequest(TestRequest request) {
        testRequests.add(request);
    }

    public TestRequest getTestByClassname(String name) {
        for (TestRequest testRequest : testRequests) {
            if (testRequest.className.equals(name)) {
                return testRequest;
            }
        }

        return null;
    }

    public boolean allTestsDone() {
        for (TestRequest testRequest : testRequests) {
            if(testRequest.getResult() == null) {
                return false;
            }
        }
        return true;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
