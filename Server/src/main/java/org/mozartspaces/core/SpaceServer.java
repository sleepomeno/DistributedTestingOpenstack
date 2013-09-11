package org.mozartspaces.core;

import at.ac.tuwien.infosys.praktikum.Util;
import org.mozartspaces.core.config.CommonsXmlConfiguration;
import org.mozartspaces.core.config.Configuration;

import java.net.URI;

/**
 *  Makes a little change opposed to {@link Server} - the default Mozartspaces configuration obviously
 *  doesn't allow to fix the IP of the space at runtime which I changed.
 */
public class SpaceServer {

    private SpaceServer() {
    }

    public static void main(final String[] args) {
        System.out.println("Starting MozartSpaces standalone peer");
        final MzsCore core;
        if (args.length == 1) {
            int port = Integer.parseInt(args[0]);
            System.out.println("Using port " + port + " for default TCP socket receiver");
            core = DefaultMzsCore.newInstance(port);
        } else {

            // Here there is my change. Replace "localhost" of default configuration with real IP
            // of this server
            Configuration config = CommonsXmlConfiguration.load();
            URI uri = URI.create("xvsm://" + Util.getLocalIP() + ":9876");
            config.setSpaceUri(uri);
            core = DefaultMzsCoreFactory.newCore(config);

        }
        System.out.println("Core is running and ready for requests");
        System.out.println("Press Ctrl+C to exit");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                core.shutdown(true);
            }
        });
    }

}
