package at.ac.tuwien.infosys.praktikum;

import at.ac.tuwien.infosys.praktikum.beans.MethodResult;
import at.ac.tuwien.infosys.praktikum.beans.TestResult;
import org.mozartspaces.core.Entry;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class Util {
    public static final String IP_FILE = "/opt/distest/ip.cfg";
    public static final int MASTER_PORT = 9876;
    public static final int TESTER_PORT = 9874;
    public static final String MASTER_IP = "localhost"; //"10.99.0.132";


    // taken from CloudScale PlatformSpecificUtil
    public static UUID tryLookupIdFromNovaName() {
        final String awsMetadataUrl = "http://169.254.169.254/latest/meta-data/hostname";

        try {
            URL metaUrl = new URL(awsMetadataUrl);
            URLConnection conn = metaUrl.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    conn.getInputStream()));
            String line = in.readLine();

            if(line == null)
                return null;

            line = line.replace(".novalocal", "");
            return UUID.fromString(line);

        } catch(IOException e) {
            throw new RuntimeException("Could not lookup UUID");
        }
    }

    public static Properties getPropertiesFromClasspath(String propFileName) {
        Properties props = new Properties();
        InputStream inputStream = Util.class.getClassLoader().getResourceAsStream(propFileName);

        if (inputStream == null) {
            throw new RuntimeException(String.format("Property file '%s' not found in the classpath", propFileName));
        }

        try {
            props.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException while loading properties file " + propFileName);
        }

        return props;
    }

    public static int getRandomNumber(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    public static String getLocalIP() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Local IP could not get fetched!");
    }

    public static String inClassPath(String name) {
        return name.replace('.', '/') + ".class";
    }

    public static String[] getTestResultHeader() {
        String[] header = { "Method", "Host", "Runtime", "Success" };
        return header;
    }

    public static String[][] getTestResultData(TestResult testResult) {
        List<MethodResult> methodResults = testResult.methodResults;
        String[][] data = new String[methodResults.size()][4];
        for (int i= 0; i < methodResults.size(); i++) {
            MethodResult methodResult = methodResults.get(i);
            String name = methodResult.method;
            long runTime = methodResult.runTime;
            boolean success = methodResult.success;
            data[i][0] = name;
            data[i][1] = testResult.hostName;
            data[i][2] = Long.toString(runTime) + " ms";
            data[i][3] = success ? "PASSED" : "FAIL";
        }

        return data;
    }

    public static <T extends Serializable> List<T> castEntries(List<? extends Serializable> entries) {
        List<T> newList = new ArrayList<T>();
        if (entries.size() == 0) return newList;

        Serializable firstEntry = entries.get(0);
        if (firstEntry instanceof Entry) {

            List<Entry> newEntries = (List<Entry>) entries;
            for (Entry entry : newEntries) {
                newList.add((T) entry.getValue());
            }
            return newList;
        } else {
            return (List<T>) entries;
        }
    }

    public static String UUIDtoOpenstackId(String clientId) {
        return clientId.replaceAll("-", " ");
    }
}
