package at.ac.tuwien.infosys.praktikum.beans;

import java.io.Serializable;

public abstract class HasLabel implements Serializable {
    public final String label;

    protected HasLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HasLabel hasLabel = (HasLabel) o;

        if (!label.equals(hasLabel.label)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }
}
