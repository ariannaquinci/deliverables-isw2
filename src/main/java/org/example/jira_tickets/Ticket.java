package org.example.jira_tickets;

public class Ticket {
    private String id;


    private int index;
    private String ov;
    private String fv;
    private String iv;


    public void setid(String id){
        this.id=id;    }
    public void setOV(String ov){
        this.ov=ov;
    }
    public void setIV(String iv){
        this.iv=iv;
    }
    public void setFV(String fv){
        this.fv=fv;
    }
    public int getIndex() {
        return this.index;
    }

    public void setIndex(int idx) {
        this.index = idx;
    }

    public String getid(){
        return this.id;
    }
    public String getOV(){
        return this.ov;
    }
    public String getIV(){
        return this.iv;
    }
    public String getFV(){
        return this.fv;
    }

}
