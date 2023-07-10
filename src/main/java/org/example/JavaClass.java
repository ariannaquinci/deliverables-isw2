package org.example;

public class JavaClass {
    private int index;
    private String path;
    private String release;

    private int size;

    private int publicMethodsCounter;
    private Commit associatedCommit;
    private int commentCounter;

    private boolean buggy;

    private int authNum;
    private int numRel;
    private long age;
    private int churn=0;
    private int changeSetSize;


    public int getChangeSetSize() {
        return changeSetSize;
    }

    public void setChangeSetSize(int changeSetSize) {
        this.changeSetSize = changeSetSize;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getAuthNum() {
        return authNum;
    }

    public void setAuthNum(int authNum) {
        this.authNum = authNum;
    }

    public int getNR() {
        return numRel;
    }

    public void setNR(int nr) {
        this.numRel = nr;
    }

    public long getAge() {
        return age;
    }

    public void setAge(long age) {
        this.age = age;
    }

    public int getCommentCounter() {
        return commentCounter;
    }

    public void setCommentCounter(int commentCounter) {
        this.commentCounter = commentCounter;
    }

    public boolean isBuggy() {
        return this.buggy;
    }

    public void setBuggy(boolean b) {
        this.buggy = b;
    }

    public int getPublicMethodsCounter() {
        return publicMethodsCounter;
    }

    public void setPublicMethodsCounter(int publicMethodsCounter) {
        this.publicMethodsCounter = publicMethodsCounter;
    }

    public Commit getAssociatedCommit() {
        return associatedCommit;
    }

    public void setAssociatedCommit(Commit associatedCommit) {
        this.associatedCommit = associatedCommit;
    }



    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }



    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }







    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }



}
