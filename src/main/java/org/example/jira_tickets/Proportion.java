package org.example.jira_tickets;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class Proportion {
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
        double prop_res=0;
        for(j=0; j<tktList.size();j++){
            if(prop[j]!=null){
                  prop_res+= prop[j];
            }

        }

        return prop_res/j;
    }

    public static double coldStart() throws IOException, ParseException {
        List<Ticket> avroTickets = RetrieveTickets.getTickets("AVRO");
        List<Ticket> openjpaTickets=RetrieveTickets.getTickets("OPENJPA");
        List<Ticket> syncopeTickets=RetrieveTickets.getTickets("SYNCOPE");
        List<Ticket> stormTickets= RetrieveTickets.getTickets("STORM");

        Double p1=getProportion(avroTickets);
        Double p2=getProportion(stormTickets);
        Double p3= getProportion(openjpaTickets);
        Double p4= getProportion(syncopeTickets);
     Double[] P= new Double[4];
        P[0]=p1;
        P[1]=p2;
        P[2]=p3;
        P[3]=p4;

        Double tmp;
        for(int i=0; i<P.length;i++){
            for(int j=i+1; j<P.length;j++){
            if(P[j]<P[i]){
                tmp=P[i];
                P[i]=P[j];
                P[j]=tmp;
            }
        }


    }
        double p=(P[1]+P[2])/2;
        return p;
    }
    public static double movingWindow(Ticket tkt, List<Ticket> tktList, int size){
        int windowSize= size*5/100;   //considero il 5% dei tickets precedenti con FV<OV

        List<Ticket> windowList=new ArrayList<Ticket>();


        double p=0;
        for(Ticket ticket: tktList) {
            // System.out.println(Integer.valueOf(ticket.getFV())<Integer.valueOf(tkt.getOV()));
            //  iterations++;

            if (windowList.size() < windowSize && Integer.valueOf(ticket.getFV()) < Integer.valueOf(tkt.getOV())) {
                windowList.add(ticket);

            }
            if(windowList.size()==windowSize) {
                p = getProportion(windowList);
            }
            // System.out.println("Number of iterations is:    "+iterations);
            return p;
        }
        return 0;


    }

}
