package com.project.stephencao.imageloader.bean;

public class FolderBean {
    private String directoryPath;
    private String firstImagePath;
    private String directoryName;
    private int imageCount;

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public String getFirstImagePath() {
        return firstImagePath;
    }

    public void setFirstImagePath(String firstImagePath) {
        this.firstImagePath = firstImagePath;
        int lastIndexOfSlash = this.firstImagePath.lastIndexOf("/");
        this.directoryName = this.firstImagePath.substring(lastIndexOfSlash + 1);
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }
}
