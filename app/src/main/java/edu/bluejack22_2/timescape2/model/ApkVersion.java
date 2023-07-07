package edu.bluejack22_2.timescape2.model;

public class ApkVersion {
    private int versionCode;
    private String versionName;
    private String fileUrl;

    public ApkVersion(){
    }

    public ApkVersion(int versionCode, String versionName, String fileUrl) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.fileUrl = fileUrl;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}

