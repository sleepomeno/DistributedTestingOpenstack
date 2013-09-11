package at.ac.tuwien.infosys.praktikum.beans;

import java.io.Serializable;
import java.util.List;

public class TestRequest implements Serializable {
    public final String className;
    public final List<String> methods;
    public final String testId;
    private TestResult result;

    public TestRequest(String className, List<String> methods, String testId) {
        this.className = className;
        this.methods = methods;
        this.testId = testId;
    }

    public TestResult getResult() {
        return result;
    }

    public void setResult(TestResult result) {
        this.result = result;
    }

    public String getKey() {
        return testId + className;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestRequest that = (TestRequest) o;

        if (!className.equals(that.className)) return false;
        if (!testId.equals(that.testId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + testId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TestRequest{" +
                "className='" + className + '\'' +
                ", methods=" + methods +
                ", testId='" + testId + '\'' +
                ", result=" + result +
                "} " + super.toString();
    }

}
