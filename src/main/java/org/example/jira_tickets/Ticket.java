package org.example.jira_tickets;

public class Ticket {
    private String id;


    private int index;
    private String OV;
    private String FV;
    private String IV;


    public void setid(String ID){
        this.id=ID;
    }
    public void setOV(String ov){
        this.OV=ov;
    }
    public void setIV(String iv){
        this.IV=iv;
    }
    public void setFV(String fv){
        this.FV=fv;
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
        return this.OV;
    }
    public String getIV(){
        return this.IV;
    }
    public String getFV(){
        return this.FV;
    }
}
