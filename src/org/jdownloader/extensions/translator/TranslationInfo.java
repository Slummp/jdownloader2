package org.jdownloader.extensions.translator;

import org.appwork.storage.Storable;

public class TranslationInfo implements Storable {
    private String id;
    private double complete;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getComplete() {
        return complete;
    }

    public void setComplete(double complete) {
        this.complete = complete;
    }

    public TranslationInfo(/* Storable */) {

    }
}
