package at.ac.tuwien.infosys.praktikum.space;

import at.ac.tuwien.infosys.praktikum.Util;
import org.mozartspaces.capi3.LabelCoordinator;
import org.mozartspaces.capi3.Selector;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.config.CommonsXmlConfiguration;
import org.mozartspaces.core.config.Configuration;
import org.mozartspaces.notifications.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SpaceUser {
    private final Logger log = LoggerFactory.getLogger(SpaceUser.class);

    public Capi capi;
    protected DefaultMzsCore core;

    private URI spaceUri;
    public NotificationManager notificationMgr;

    // .ctor for Local Space User
    private SpaceUser(int port, String host) {
        initCapiCommunication(port, true);
        this.spaceUri = URI.create("xvsm://" + host + ":" + port);

        initializeContainers(host, port);
    }

    // .ctor for Remote Space User
    private SpaceUser(int port) {
        initCapiCommunication(port, false);

        // TODO local

        SpaceLocation spaceLocation = getSpaceLocation();

        String host = spaceLocation.host;
        int spacePort = spaceLocation.port;
//        String host = Util.MASTER_IP;
//        int spacePort = Util.MASTER_PORT;
        initializeContainers(host, spacePort);
        this.spaceUri = URI.create("xvsm://" + host + ":" + spacePort);
    }

    // .ctor for Tester Space User
    private SpaceUser(int port, boolean embedded) {
        initCapiCommunication(port, embedded);

        String host = Util.MASTER_IP;
        int spacePort = Util.MASTER_PORT;
        initializeContainers(host, spacePort);
        this.spaceUri = URI.create("xvsm://" + host + ":" + spacePort);
    }

    public static SpaceUser createLocalSpaceUser(int port) {
        return new SpaceUser(port, Util.getLocalIP());
    }

    public static SpaceUser createRemoteSpaceUser(int port) {
        return new SpaceUser(port);
    }

    public static SpaceUser createSpaceUserWithoutLocationLookup(int port) {
        return new SpaceUser(port, false);
    }

    protected void initCapiCommunication(int port, boolean embedded) {
        try {
            Configuration config = CommonsXmlConfiguration.load(port);
            String hostAddress = Util.getLocalIP();

            log.debug("My hostAddress is " + hostAddress);

            URI space = URI.create("xvsm://" + hostAddress + ":" + port);
            config.setSpaceUri(space);
            config.setEmbeddedSpace(embedded);
            core = DefaultMzsCore.newInstance(config);
            capi = new Capi(core);
            notificationMgr = new NotificationManager(core);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Space connection could not be established!");
            System.exit(1);
        }
    }

    public void writeToSpace(SpaceCommunication comm) {
        List<? extends Serializable> items = (comm.item != null) ? Arrays.asList(comm.item) : comm.items;

        try {
            List<Entry> entries = new ArrayList<Entry>();
            for (Serializable item : items) {
                Entry entry = (comm.label != null) ? new Entry(item, LabelCoordinator.newCoordinationData(comm.label)) : new Entry(item);
                entries.add(entry);
            }
            capi.write(entries, comm.container, comm.timeout, comm.tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeContainers(String host, int spacePort) {
        for(ContainerHolder holder : ContainerManager.CONTAINERS) {
            holder.initialize(capi, host, spacePort);
        }
    }

    private SpaceLocation getSpaceLocation() {
        File spaceLocationFile = new File(Util.IP_FILE);
        try {
            Scanner scanner = new Scanner(spaceLocationFile).useDelimiter(":|" + System.lineSeparator());
            if (spaceLocationFile.exists() && spaceLocationFile.isFile()) {
                String hostname = scanner.next();
                int port = scanner.nextInt();

                SpaceLocation spaceLocation = new SpaceLocation(hostname, port);

                log.info(String.format("Space location is %s", spaceLocation));

                return spaceLocation;
            }
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        throw new RuntimeException("Could not parse Space Location!");
    }

    public <T extends Serializable> List<T> takeFromSpace(SpaceCommunication comm) {
        List<Selector> selectors = (comm.selector != null) ? Arrays.asList(comm.selector) : comm.selectors;

        try {
            return capi.take(comm.container, selectors, comm.timeout, comm.tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public URI getSpaceUri() {
        return spaceUri;
    }

    public  <T extends Serializable> List<T>  readFromSpace(SpaceCommunication comm) {
        List<Selector> selectors = (comm.selector != null) ? Arrays.asList(comm.selector) : comm.selectors;

        try {
            return capi.read(comm.container, selectors, comm.timeout, comm.tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
