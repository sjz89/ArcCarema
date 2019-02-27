package com.daylight.arccarema;

import org.litepal.crud.DataSupport;

public class Feature extends DataSupport{
    private byte[] data;
    private Face face;

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setFace(Face face) {
        this.face = face;
    }

    public byte[] getData() {
        return data;
    }

    public Face getFace() {
        return face;
    }
}
