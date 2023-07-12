package org.example.jira_tickets;

import org.example.releases.GetReleaseInfo;
import org.example.utils.CsvManager;
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

import static org.example.MainClass.getProjName;
import static org.example.utils.JSONManager.readJsonFromUrl;
public class RetrieveTickets {
    private RetrieveTickets(){}
    private static final String VERSION_CSV="VersionInfo.csv";

    public static List<Ticket> getTickets(String projName) throws IOException, JSONException {
        List<Ticket> tickets= new ArrayList<Ticket>();
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
            String IV = null;
            
            for(i=0; i<total; i++){
                    Ticket tkt= new Ticket();
                    JSONObject index = issues.getJSONObject(i%j);
                    JSONObject fields= index.getJSONObject("fields");
                    JSONArray versions= fields.getJSONArray("versions");

                    LocalDateTime resDate= LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0,16));

                    String id= index.get("key").toString();
                    String FV=setIndexVDate(resDate.toLocalDate().atStartOfDay().toString(), projName);


                    LocalDateTime creatDate=LocalDateTime.parse(fields.get("created").toString().substring(0,16)).toLocalDate().atStartOfDay();;
                    String OV= setIndexVDate(creatDate.toLocalDate().atStartOfDay().toString(), projName);
                    if(!versions.isEmpty()){
                        JSONObject IVIndex= versions.getJSONObject(0);
                        injectedVersion= IVIndex.get("name").toString();
                        IV=setIndexIV(injectedVersion, projName);
                    }
                    else{
                        IV=null;
                    }
                    assignVersions(IV,OV,FV,id,tkt,tickets,count);

            }





        return tickets;
}
private static void assignVersions(String IV, String OV, String FV, String id, Ticket tkt, List<Ticket> tickets, int count){
    if((FV!=null && OV!=null )&& !(FV.compareTo("1")==0 &&OV.compareTo("1")==0) &&(IV==null || IV.compareTo(FV)<0) && OV.compareTo(FV)<=0){
            count++;
            tkt.setIndex(count);
            tkt.setFV(FV);
            tkt.setid(id);
            tkt.setOV(OV);
            tickets.add(tkt);
            if(IV!=null && Integer.parseInt(IV)>Integer.parseInt(OV)){
                IV=null;
            }
            tkt.setIV(IV);

        }
    }

    private static String setIndexVDate(String versionDate, String projName) {
        String V=null;
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
                String versionName = columns[2];
                String versDate=columns[3];

                if(versionDate.compareTo(versDate)<=0){
                    return index;
                }


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return V;
    }

    public static String setIndexIV(String version, String project) throws  JSONException{
        String V=null;
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
                 String versDate=columns[3];

                 if(version.compareTo(versionName)==0){
                     return index;
                 }


             }

         } catch (IOException e) {
             e.printStackTrace();
         }
        return V;

    }
    public static void assignIV(List<Ticket> tktList) throws IOException, ParseException {
        double P = -1;
        int counter = 0;
        int IV = 0;
        double P_coldStart = Proportion.coldStart();
        List<Ticket> IvNotNullTickets = new ArrayList<Ticket>();
        List<Ticket> IvNullTickets = new ArrayList<Ticket>();

        splitTicketsByIV(tktList, IvNotNullTickets, IvNullTickets);

        for (Ticket tkt : IvNullTickets) {
            counter = calculateCounter(tkt, IvNotNullTickets);

            if (counter >= tktList.size() * 5 / 100) {
                P = Proportion.movingWindow(tkt, IvNotNullTickets, tktList.size());
                IV = calculateIV(tkt, P);
            } else {
                IV = calculateIV(tkt, P_coldStart);
            }

            if (IV == 0) {
                IV = 1;
            }

            tkt.setIV(String.valueOf(IV));
        }
    }

    private static void splitTicketsByIV(List<Ticket> tktList, List<Ticket> IvNotNullTickets,
                                         List<Ticket> IvNullTickets) {
        for (Ticket tkt : tktList) {
            if (tkt.getIV() != null) {
                IvNotNullTickets.add(tkt);
            } else {
                IvNullTickets.add(tkt);
            }
        }
    }

    private static int calculateCounter(Ticket tkt, List<Ticket> IvNotNullTickets) {
        int counter = 0;

        for (Ticket ticket : IvNotNullTickets) {
            if (Integer.valueOf(tkt.getOV()) > Integer.valueOf(ticket.getFV())) {
                counter++;
            }
        }

        return counter;
    }

    private static int calculateIV(Ticket tkt, double P) {
        int IV;

        if (tkt.getOV().compareTo(tkt.getFV()) == 0) {
            IV = (int) (Double.valueOf(tkt.getFV()) - P);
        } else {
            IV = (int) (Double.valueOf(tkt.getFV()) - (Double.valueOf(tkt.getFV()) - Double.valueOf(tkt.getOV())) * P);
        }

        return IV;
    }

    public static List<Ticket> selectValidTickets(List<Ticket> tktList) throws FileNotFoundException {
            List<Ticket> newTktList= new ArrayList<Ticket>();

            for(Ticket tkt: tktList){
                if( Integer.valueOf(tkt.getIV())<Integer.valueOf(tkt.getFV()) ){
                    newTktList.add(tkt);

                }

            }
            return newTktList;
    }

   }




