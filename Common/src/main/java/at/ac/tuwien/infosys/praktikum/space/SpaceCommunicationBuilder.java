package at.ac.tuwien.infosys.praktikum.space;

import org.mozartspaces.capi3.Selector;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.TransactionReference;

import java.io.Serializable;
import java.util.List;

public class SpaceCommunicationBuilder {
    private ContainerReference container;
    private List<Serializable> items;
    private long timeout = MzsConstants.RequestTimeout.DEFAULT;
    private TransactionReference tx;
    private List<Selector> selectors;
    private Serializable item;
    private String label;
    private Selector selector;

    public SpaceCommunicationBuilder setContainer(ContainerReference container) {
        this.container = container;
        return this;
    }

    public SpaceCommunicationBuilder setItems(List<Serializable> items) {
        this.items = items;
        return this;
    }

    public SpaceCommunicationBuilder setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public SpaceCommunicationBuilder setTx(TransactionReference tx) {
        this.tx = tx;
        return this;
    }

    public SpaceCommunicationBuilder setSelectors(List<Selector> selectors) {
        this.selectors = selectors;
        return this;
    }

    public SpaceCommunicationBuilder setItem(Serializable item) {
        this.item = item;
        return this;
    }

    public SpaceCommunicationBuilder setLabel(String label) {
        this.label = label;
        return this;
    }

    public SpaceCommunication build() {
        return new SpaceCommunication(item, container, items, timeout, tx, selectors, label, selector);
    }

    public SpaceCommunicationBuilder setSelector(Selector selector) {
        this.selector = selector;
        return this;
    }
}