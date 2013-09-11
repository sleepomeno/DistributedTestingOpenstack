package at.ac.tuwien.infosys.praktikum.space;

import at.ac.tuwien.infosys.praktikum.Util;
import at.ac.tuwien.infosys.praktikum.beans.ResourceRequest;
import at.ac.tuwien.infosys.praktikum.beans.ResourceResponse;
import org.mozartspaces.capi3.LabelCoordinator;
import org.mozartspaces.core.CapiUtil;
import org.mozartspaces.core.MzsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpaceRemoteClassLoader extends ClassLoader {
    private final Logger log = LoggerFactory.getLogger(SpaceRemoteClassLoader.class);
    private final SpaceUser spaceAgent;

    private final Map<String, ByteBuffer> classtoBytes = new HashMap<>();

    /**
     * Serves as a Space selector "label" and makes it clear from whom or for whom the request or response is
     */
    private final String uuid;

    public SpaceRemoteClassLoader(SpaceUser spaceAgent, String uuid, ClassLoader parentClassLoader) {
        super(parentClassLoader);
        this.spaceAgent = spaceAgent;
        this.uuid = uuid;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        log.debug("Want to load: " + name);
        Class<?> definedClass;
        if (classtoBytes.containsKey(name)) {
            log.info(String.format("ClassToBytes containsKey %s", name));
            byte[] content = classtoBytes.get(name).array();
            definedClass = defineClass(name, content, 0, content.length);
        } else {
            log.info(String.format("ClassToBytes DOES NOT contain key %s", name));
            try {
                definedClass = super.findClass(name);
            } catch (ClassNotFoundException e) {
                log.debug(String.format("Parent classloader does not find %s", name));
                ResourceRequest classRequest = new ResourceRequest(name, uuid + name, Util.inClassPath(name));

                // Write the class request to the space
                SpaceCommunication requestComm = new SpaceCommunicationBuilder().setContainer(ContainerManager.classRequests.getContainer())
                        .setItem(classRequest).build();
                spaceAgent.writeToSpace(requestComm);

                log.debug("Made space class request for " + name);

                // Wait for an answer
                SpaceCommunication comm = new SpaceCommunicationBuilder().setContainer(ContainerManager.classResponses.getContainer())
                        .setTimeout(MzsConstants.RequestTimeout.INFINITE)
                        .setSelector(LabelCoordinator.newSelector(uuid + name, 1)).build();
                List<ResourceResponse> classes = spaceAgent.takeFromSpace(comm);

                // Define the received class
                ResourceResponse clazz = CapiUtil.getSingleEntry(classes);
                log.debug("Got a space class response for " + clazz.name);

                definedClass = defineClass(clazz.name, clazz.content, 0, clazz.content.length);
            }
        }
        return definedClass;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream resourceOfParentClassloader = getParent().getResourceAsStream(name);
        if(resourceOfParentClassloader != null) return resourceOfParentClassloader;

        ResourceRequest req = new ResourceRequest(name, uuid, name);
        SpaceCommunication resourceReq = new SpaceCommunicationBuilder().setContainer(ContainerManager.resourceRequests.getContainer()).setItem(req).build();
        spaceAgent.writeToSpace(resourceReq);

        log.debug("Made space resource request for " + name);

        SpaceCommunication resourceResp = new SpaceCommunicationBuilder().setContainer(ContainerManager.resourceResponses.getContainer())
                .setTimeout(MzsConstants.RequestTimeout.INFINITE)
                .setSelector(LabelCoordinator.newSelector(uuid, 1)).build();
        List<ResourceResponse> resources = spaceAgent.takeFromSpace(resourceResp);

        ResourceResponse response = CapiUtil.getSingleEntry(resources);
        log.debug("Got a space resource response for " + response.name);

        return new ByteArrayInputStream(response.content);
    }


    public void addClass(String className, byte[] content) {
        classtoBytes.put(className, ByteBuffer.wrap(content));
    }
}
