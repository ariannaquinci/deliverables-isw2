package org.example;

import org.eclipse.jgit.revwalk.RevCommit;
import org.example.jira_tickets.Ticket;
import org.example.utils.CsvManager;

import java.io.FileNotFoundException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.MainClass.getProjName;


public class MergeTicketsAndCommits {
    //private constructor
    private MergeTicketsAndCommits(){
        //no operation
    }

    public static void addCommitsOfFirstRelease(Set<RevCommit> commits, Set<Commit> commitList) throws FileNotFoundException {
        Commit cmt= new Commit();

        String rel1Date=CsvManager.readCsvEntry(getProjName()+"VersionInfo.csv",3,0, "1");

        for(RevCommit commit: commits){
         if( String.valueOf(ZonedDateTime.ofInstant(commit.getAuthorIdent().getWhen().toInstant(), ZoneId.systemDefault()))
                .compareTo(rel1Date)<=0){
            cmt.setId(commit.getId().toString());
            cmt.setRevCommit(commit);
            cmt.setRelease("1");

            commitList.add(cmt);
        }
        }
    }

    public static List<Commit> createCommitList(Ticket tkt, Set<RevCommit> commits) {

            Pattern pattern;
            Matcher matcher;
            List<Commit> associatedCommits=new ArrayList<>();


            Commit cmt= new Commit();


            for(RevCommit commit: commits) {

                        pattern = Pattern.compile(tkt.getid());
                        matcher = pattern.matcher(commit.getFullMessage());
                        if (matcher.find()) {
                            cmt.setId(commit.getId().toString());
                            cmt.setRevCommit(commit);
                            cmt.setAssociatedTicket(tkt);
                            cmt.setRelease(tkt.getFV());
                            associatedCommits.add(cmt);

                        }


                    }

            return associatedCommits;


    }






}
