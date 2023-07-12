package org.example.jira_tickets;

import org.example.releases.GetReleaseInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.example.utils.JSONManager.readJsonFromUrl;
public class RetrieveTickets {
    private RetrieveTickets(){}
    private static final String VERSION_CSV="VersionInfo.csv";

    public static List<Ticket> getTickets(String projName) throws IOException, JSONException {
        List<Ticket> tickets= new ArrayList<>();
        int count=0;
        Integer i = 0;
        Integer total = 1;
        Integer j;
        JSONArray issues;
        j = 1000;
        GetReleaseInfo.getReleaseInfo(projName);

            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"+projName+"%22AND%22issueType%22=%22bug%22AND" +
                    "(%22status%22=%22closed%22OR%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22" +
                    "&fields=key,fixVersion,releaseDate,resolutiondate,versions,created&startAt=" + i+ "&maxResults=" + j;
            JSONObject json = readJsonFromUrl(url);
            issues = json.getJSONArray("issues");
            total = json.getInt("total");

            String injectedVersion =null;
            String iv = null;
            
            for(i=0; i<total; i++){
                    Ticket tkt= new Ticket();
                    JSONObject index = issues.getJSONObject(i%j);
                    JSONObject fields= index.getJSONObject("fields");
                    JSONArray versions= fields.getJSONArray("versions");

                    LocalDateTime resDate= LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0,16));

                    String id= index.get("key").toString();
                    String fv=setIndexVDate(resDate.toLocalDate().atStartOfDay().toString(), projName);


                    LocalDateTime creatDate=LocalDateTime.parse(fields.get("created").toString().substring(0,16)).toLocalDate().atStartOfDay();
                    String ov= setIndexVDate(creatDate.toLocalDate().atStartOfDay().toString(), projName);
                    if(!versions.isEmpty()){
                        JSONObject ivIndex= versions.getJSONObject(0);
                        injectedVersion= ivIndex.get("name").toString();
                        iv=setIndexIV(injectedVersion, projName);
                    }
                    else{
                        iv=null;
                    }
                    assignVersions(iv,ov,fv,id,tkt,tickets,count);

            }





        return tickets;
}
private static void assignVersions(String iv, String ov, String fv, String id, Ticket tkt, List<Ticket> tickets, int count){
    if((fv!=null && ov!=null )&& !(fv.compareTo("1")==0 &&ov.compareTo("1")==0) &&(iv==null || iv.compareTo(fv)<0) && ov.compareTo(fv)<=0){
            count++;
            tkt.setIndex(count);
            tkt.setFV(fv);
            tkt.setid(id);
            tkt.setOV(ov);
            tickets.add(tkt);
            if(iv!=null && Integer.parseInt(iv)>Integer.parseInt(ov)){
                iv=null;
            }
            tkt.setIV(iv);

        }
    }

    private static String setIndexVDate(String versionDate, String projName) {
        String v=null;
        boolean isFirstLine = true;
        try (BufferedReader br = new BufferedReader(new FileReader(projName+VERSION_CSV))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] columns = line.split(",");
                String index = columns[0];

                String versDate=columns[3];

                if(versionDate.compareTo(versDate)<=0){
                    return index;
                }


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return v;
    }

    public static String setIndexIV(String version, String project) throws  JSONException{
        String v=null;
        boolean isFirstLine = true;
        try (BufferedReader br = new BufferedReader(new FileReader(project+VERSION_CSV))) {
             String line;
             while ((line = br.readLine()) != null) {
                 if (isFirstLine) {
                     isFirstLine = false;
                     continue;
                 }

                 String[] columns = line.split(",");
                 String index = columns[0];
                 String versionName = columns[2];


                 if(version.compareTo(versionName)==0){
                     return index;
                 }


             }

         } catch (IOException e) {
             e.printStackTrace();
         }
        return v;

    }
    public static void assignIV(List<Ticket> tktList) throws IOException, ParseException {
        double p = -1;
        int counter = 0;
        int iv = 0;
        double pColdStart = Proportion.coldStart();
        List<Ticket> ivNotNullTickets = new ArrayList<>();
        List<Ticket> ivNullTickets = new ArrayList<>();

        splitTicketsByIV(tktList, ivNotNullTickets, ivNullTickets);

        for (Ticket tkt : ivNullTickets) {
            counter = calculateCounter(tkt, ivNotNullTickets);

            if (counter >= tktList.size() * 5 / 100) {
                p= Proportion.movingWindow(tkt, ivNotNullTickets, tktList.size());
                iv = calculateIV(tkt, p);
            } else {
                iv = calculateIV(tkt, pColdStart);
            }

            if (iv == 0) {
                iv = 1;
            }

            tkt.setIV(String.valueOf(iv));
        }
    }

    private static void splitTicketsByIV(List<Ticket> tktList, List<Ticket> ivNotNullTickets,
                                         List<Ticket> ivNullTickets) {
        for (Ticket tkt : tktList) {
            if (tkt.getIV() != null) {
                ivNotNullTickets.add(tkt);
            } else {
                ivNullTickets.add(tkt);
            }
        }
    }

    private static int calculateCounter(Ticket tkt, List<Ticket> ivNotNullTickets) {
        int counter = 0;

        for (Ticket ticket : ivNotNullTickets) {
            if (Integer.valueOf(tkt.getOV()) > Integer.valueOf(ticket.getFV())) {
                counter++;
            }
        }

        return counter;
    }

    private static int calculateIV(Ticket tkt, double p) {
        int iv;

        if (tkt.getOV().compareTo(tkt.getFV()) == 0) {
            iv = (int) (Double.valueOf(tkt.getFV()) - p);
        } else {
            iv = (int) (Double.valueOf(tkt.getFV()) - (Double.valueOf(tkt.getFV()) - Double.valueOf(tkt.getOV())) * p);
        }

        return iv;
    }

    public static List<Ticket> selectValidTickets(List<Ticket> tktList)  {
            List<Ticket> newTktList= new ArrayList<>();

            for(Ticket tkt: tktList){
                if( Integer.valueOf(tkt.getIV())<Integer.valueOf(tkt.getFV()) ){
                    newTktList.add(tkt);

                }

            }
            return newTktList;
    }

   }




