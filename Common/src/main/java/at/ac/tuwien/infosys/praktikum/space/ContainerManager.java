package at.ac.tuwien.infosys.praktikum.space;

import org.mozartspaces.capi3.*;

import java.util.Arrays;
import java.util.List;

public class ContainerManager {
    public static final ContainerHolder hostTests = new ContainerHolder("hostTests", asList(new LabelCoordinator()));
    public static final ContainerHolder masterTests = new ContainerHolder("masterTests", asList(new FifoCoordinator()));
    public static final ContainerHolder classRequests = new ContainerHolder("classRequests", asList(new AnyCoordinator()));
    public static final ContainerHolder classResponses = new ContainerHolder("classResponses", asList(new LabelCoordinator()));
    public static final ContainerHolder resourceRequests = new ContainerHolder("resourceRequests", asList(new AnyCoordinator()));
    public static final ContainerHolder resourceResponses = new ContainerHolder("resourceResponses", asList(new LabelCoordinator()));
    public static final ContainerHolder bootComplete = new ContainerHolder("bootComplete", asList(new LifoCoordinator(), new LabelCoordinator()));
    public static final ContainerHolder testResults = new ContainerHolder("testResults", asList(new LabelCoordinator(), new AnyCoordinator()));
    public static final ContainerHolder clientResults = new ContainerHolder("clientResults", asList(new LabelCoordinator()));

    public final static List<ContainerHolder> CONTAINERS = Arrays.asList(hostTests, classRequests, classResponses, resourceRequests, clientResults, resourceResponses, bootComplete, testResults, masterTests);

    private static List<Coordinator> asList(Coordinator... coordinators) {
        return Arrays.asList(coordinators);
    }
}
