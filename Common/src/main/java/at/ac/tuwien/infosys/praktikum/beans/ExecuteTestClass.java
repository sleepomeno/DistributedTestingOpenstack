package at.ac.tuwien.infosys.praktikum.beans;

import java.io.Serializable;
import java.util.List;

public class ExecuteTestClass implements Serializable {
    public final byte[] content;
    public final String testId;
    public final String suiteClassName;
    public final String name;
    public final List<String> methods;

    @Override
    public String toString() {
        return "ExecuteTestClass{" +
                "testId='" + testId + '\'' +
                ", suiteClassName='" + suiteClassName + '\'' +
                ", name='" + name + '\'' +
                ", methods=" + methods +
                '}';
    }

    public ExecuteTestClass(byte[] content, String testId, String suiteClassName, String name, List<String> methods) {
        this.content = content;
        this.testId = testId;
        this.suiteClassName = suiteClassName;
        this.name = name;
        this.methods = methods;
    }
}
