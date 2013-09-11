package at.ac.tuwien.infosys.praktikum.beans;

import java.util.List;

public class HostExecuteTest extends ResourceResponse {
    public final List<String> testMethods;
    public final String testId;

    public HostExecuteTest(byte[] content, String name, List<String> testMethods, String testId) {
        super(name, content);

        this.testMethods = testMethods;
        this.testId = testId;
    }

    @Override
    public String toString() {
        return "ExecuteTest{" +
                "testMethods=" + testMethods +
                ", testId='" + testId + '\'' +
                '}' + "\n" + super.toString();
    }
}
