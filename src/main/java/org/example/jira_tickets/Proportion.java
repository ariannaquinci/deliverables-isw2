package org.example.jira_tickets;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class Proportion {
   private Proportion(){
       //private constructor to hyde the public implicit one
   }
    public static double getProportion( List<Ticket> tktList){
        Double[] prop= new Double[tktList.size()];
        int i=0;
        String iv;
        String fv;
        String ov;
        double tmp=0;
        for(Ticket tkt: tktList){
            iv=tkt.getIV();
            fv= tkt.getFV();
            ov=tkt.getOV();
            if(iv!=null && fv.compareTo(ov)!=0){
                tmp =(Double.parseDouble(fv)-Double.parseDouble(iv))/(Double.parseDouble(fv)-Double.parseDouble(ov));
            }
            else if(iv!=null &&fv.compareTo(ov)==0){
                tmp=Double.parseDouble(fv)-Double.parseDouble(iv);

            }
            else{tmp=0;}
            if(tmp>=1){
                prop[i]=tmp;
                i++;
            }
}

        int j;
        double res=0;
        for(j=0; j<tktList.size();j++){
            if(prop[j]!=null){
                  res+= prop[j];
            }

        }
        if(j!=0){
        return res/j;}
        else return 0;
    }

    public static double coldStart() throws IOException{
        List<Ticket> avroTickets = RetrieveTickets.getTickets("AVRO");
        List<Ticket> openjpaTickets=RetrieveTickets.getTickets("OPENJPA");
        List<Ticket> syncopeTickets=RetrieveTickets.getTickets("SYNCOPE");
        List<Ticket> stormTickets= RetrieveTickets.getTickets("STORM");

        Double p1=getProportion(avroTickets);
        Double p2=getProportion(stormTickets);
        Double p3= getProportion(openjpaTickets);
        Double p4= getProportion(syncopeTickets);
        Double[] prop= new Double[4];
        prop[0]=p1;
        prop[1]=p2;
        prop[2]=p3;
        prop[3]=p4;

        Double tmp;
        for(int i=0; i<prop.length;i++){
            for(int j=i+1; j<prop.length;j++){
            if(prop[j]<prop[i]){
                tmp=prop[i];
                prop[i]=prop[j];
                prop[j]=tmp;
            }
        }


    }
        return (prop[1]+prop[2])/2;

    }
    public static double movingWindow(Ticket tkt, List<Ticket> tktList, int size){
        int windowSize= size*5/100;   //considero il 5% dei tickets precedenti con FV<OV

        List<Ticket> windowList=new ArrayList<>();


        double p=0;
        for(Ticket ticket: tktList) {

            if (windowList.size() < windowSize && Integer.valueOf(ticket.getFV()) < Integer.valueOf(tkt.getOV())) {
                windowList.add(ticket);

            }
            if(windowList.size()==windowSize) {
                p = getProportion(windowList);
            }

        }
        return p;


    }

}
