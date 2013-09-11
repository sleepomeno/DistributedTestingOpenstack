package at.ac.tuwien.infosys.praktikum.beans;

import java.util.List;

public class TestResult extends HasLabel {
    public final List<MethodResult> methodResults;
    public final String hostName;
    public final boolean success;
    public final String testClassName;

    public TestResult(String testId, boolean success, List<MethodResult> methodResults, String hostName, String testClassName) {
        super(testId);
        this.methodResults = methodResults;
        this.hostName = hostName;
        this.success = success;
        this.testClassName = testClassName;
    }

    public String getKey() {
        return label + testClassName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TestResult that = (TestResult) o;

        if (success != that.success) return false;
        if (!hostName.equals(that.hostName)) return false;
        if (!methodResults.equals(that.methodResults)) return false;
        if (!testClassName.equals(that.testClassName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + methodResults.hashCode();
        result = 31 * result + hostName.hashCode();
        result = 31 * result + (success ? 1 : 0);
        result = 31 * result + testClassName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "methodResults=" + methodResults +
                ", hostName='" + hostName + '\'' +
                ", success=" + success +
                ", testClassName='" + testClassName + '\'' +
                "} " + super.toString();
    }
}
