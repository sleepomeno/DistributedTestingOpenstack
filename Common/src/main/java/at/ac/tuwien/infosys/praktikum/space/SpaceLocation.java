package at.ac.tuwien.infosys.praktikum.space;

public class SpaceLocation {
    public final String host;
    public final int port;

    public SpaceLocation(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return "SpaceLocation{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
