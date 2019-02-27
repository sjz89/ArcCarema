package com.daylight.arccarema;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class Face extends DataSupport{
    private int id;
    private String name;
    private List<Feature> featureList=new ArrayList<>();

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFeatureList(List<Feature> featureList) {
        this.featureList = featureList;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Feature> getFeatureList() {
        return featureList;
    }
}
