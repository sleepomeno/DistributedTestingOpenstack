package at.ac.tuwien.infosys.praktikum.beans;

import java.util.Date;

public class BootComplete extends HasLabel {
    public final Date date;

    public BootComplete(String label, Date date) {
        super(label);

        this.date = date;
    }
}
