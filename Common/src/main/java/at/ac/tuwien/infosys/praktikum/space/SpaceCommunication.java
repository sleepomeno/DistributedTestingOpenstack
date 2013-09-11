package at.ac.tuwien.infosys.praktikum.space;

import org.mozartspaces.capi3.Selector;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.TransactionReference;

import java.io.Serializable;
import java.util.List;

public class SpaceCommunication {
    public final ContainerReference container;
    public final List<Serializable> items;
    public long timeout = MzsConstants.RequestTimeout.DEFAULT;
    public final TransactionReference tx;
    public final List<Selector> selectors;
    public final Serializable item;
    public final String label;
    public final Selector selector;

    public SpaceCommunication(Serializable item, ContainerReference container, List<Serializable> items, long timeout, TransactionReference tx, List<Selector> selectors, String label, Selector selector) {
        this.container = container;
        this.items = items;
        this.item = item;
        this.timeout = timeout;
        this.tx = tx;
        this.selectors = selectors;
        this.label = label;
        this.selector = selector;
    }
}
