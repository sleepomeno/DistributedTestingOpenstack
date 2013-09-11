package at.ac.tuwien.infosys.praktikum.beans;


import java.io.Serializable;

public class MethodResult implements Serializable {
    public final String failure;
    public final long runTime;
    public final String method;
    public final boolean success;

    public MethodResult(String methodName, long runTime, String failure, boolean success) {
        this.method = methodName;
        this.runTime = runTime;
        this.failure = failure;
        this.success = success;

    }

    @Override
    public String toString() {
        return "MethodResult{" +
                "failure='" + failure + '\'' +
                ", runTime=" + runTime +
                ", method='" + method + '\'' +
                ", success=" + success + "}";
    }
}
