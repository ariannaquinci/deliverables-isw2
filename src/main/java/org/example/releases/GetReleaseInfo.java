package org.example.releases;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.example.utils.JSONManager.readJsonFromUrl;


public class GetReleaseInfo {
    private static Map<LocalDateTime, String> releaseNames;
    private static Map<LocalDateTime, String> releaseID;
    private static List<LocalDateTime> releases;


    private GetReleaseInfo(){}

    public static void getReleaseInfo(String projName) throws IOException, JSONException {

        //Fills the arraylist with releases dates and orders them
        //Ignores releases with missing dates
        releases = new ArrayList<LocalDateTime>();
        Integer i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");
        releaseNames = new HashMap<>();
        releaseID = new HashMap<> ();
        for (i = 0; i < versions.length(); i++ ) {

            String name = "";
            String id = "";
            if(versions.getJSONObject(i).get("released").equals(true) &&  versions.getJSONObject(i).has("releaseDate")) {
                name=getName(versions,i);
                id=getId(versions,i);
                addRelease(versions.getJSONObject(i).get("releaseDate").toString(),name,id);
            }
        }

    // order releases by date
        Collections.sort(releases, Comparable::compareTo);

        if (releases.size() < 6)
            return;
        String outname = projName + "VersionInfo.csv";
		try (FileWriter fileWriter= new FileWriter(outname)){

            fileWriter.append("Index,Version ID,Version Name,Date");
            fileWriter.append("\n");
            for ( i = 0; i < releases.size(); i++) {
                Integer index = i + 1;
                fileWriter.append(index.toString());
                fileWriter.append(",");
                fileWriter.append(releaseID.get(releases.get(i)));
                fileWriter.append(",");
                fileWriter.append(releaseNames.get(releases.get(i)));
                fileWriter.append(",");
                fileWriter.append(releases.get(i).toString());
                fileWriter.append("\n");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    public static void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime))
            releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseID.put(dateTime, id);

    }

    private static String getName(JSONArray versions, int i) {

        if (versions.getJSONObject(i).has("name"))
            return versions.getJSONObject(i).get("name").toString();
        return "";
    }
    private static String getId(JSONArray versions, int i){
        if (versions.getJSONObject(i).has("id"))
           return versions.getJSONObject(i).get("id").toString();
        return "";
    }
    }



