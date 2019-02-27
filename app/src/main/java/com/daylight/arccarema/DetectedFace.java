package com.daylight.arccarema;

import org.litepal.crud.DataSupport;

public class DetectedFace extends DataSupport {
    private int id;
    private byte[] face;
    private byte[] feature;

    public void setId(int id) {
        this.id = id;
    }

    public void setFace(byte[] face) {
        this.face = face;
    }

    public void setFeature(byte[] feature) {
        this.feature = feature;
    }

    public int getId() {
        return id;
    }

    public byte[] getFace() {
        return face;
    }

    public byte[] getFeature() {
        return feature;
    }
}
