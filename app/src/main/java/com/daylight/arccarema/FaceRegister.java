package com.daylight.arccarema;

import com.arcsoft.facerecognition.AFR_FSDKFace;

import java.util.ArrayList;
import java.util.List;

public class FaceRegister {
    private String name;
    private List<AFR_FSDKFace> faceList;
    FaceRegister(String name){
        this.name=name;
        faceList =new ArrayList<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFaceList(List<AFR_FSDKFace> faceList) {
        this.faceList = faceList;
    }

    public String getName() {
        return name;
    }

    public List<AFR_FSDKFace> getFaceList() {
        return faceList;
    }
}
