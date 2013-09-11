package at.ac.tuwien.infosys.praktikum.beans;

public class ResourceRequest extends HasLabel {
    public final String identifier;
    public final String classPath;

    public ResourceRequest(String identifier, String label, String classPath) {
        super(label);

        this.identifier = identifier;
        this.classPath = classPath;
    }
}
