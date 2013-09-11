package at.ac.tuwien.infosys.praktikum.space;

import org.mozartspaces.capi3.Coordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.CapiUtil;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.MzsCoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

public class ContainerHolder {

    private final Logger log = LoggerFactory.getLogger(ContainerHolder.class);

    private final String containerName;
    private ContainerReference containerRef;

    private List<Coordinator> coordinators;

    public ContainerHolder(String containerName) {
        this.containerName = containerName;
    }

    public ContainerHolder(String containerName, List<Coordinator> coordinators) {
        this.containerName = containerName;
        this.coordinators = coordinators;
    }

    public ContainerReference initialize(Capi capi, String serverAddr, int port) {
        if (containerRef == null) {
            try {
                final String address = String.format("xvsm://%s:%d", serverAddr, port);
                URI space = URI.create(address);
                log.warn("Space URI is " + space.toString());
                containerRef = CapiUtil.lookupOrCreateContainer(containerName, space, coordinators, null, capi);
            } catch (MzsCoreException e) {
                log.error(e.getMessage());
                log.error("Space container reference could not be established!");
                System.exit(1);
            }
        }
        return containerRef;
    }

    public ContainerReference getContainer() {
        return containerRef;
    }
}
