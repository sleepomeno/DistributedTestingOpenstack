package at.ac.tuwien.infosys.praktikum.beans;

import java.io.Serializable;

public class ResourceResponse implements Serializable {
    public final String name;
    public final byte[] content;

    public ResourceResponse(String name, byte[] content) {
        this.name = name;
        this.content = content;
    }

    @Override
    public String toString() {
        return "ResourceResponse{" +
                "name='" + name + '\'' +
                '}';
    }
}
