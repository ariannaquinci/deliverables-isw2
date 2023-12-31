package org.example;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.example.jira_tickets.RetrieveTickets;
import org.example.jira_tickets.Ticket;
import org.example.utils.GithubRepoUtilities;

import java.util.*;

import static org.example.MergeTicketsAndCommits.addCommitsOfFirstRelease;
import static org.example.RetrieveWekaInformations.evaluateWalkForward;
import static org.example.jira_tickets.RetrieveTickets.assignIV;
import static org.example.jira_tickets.RetrieveTickets.getTickets;
import static org.example.utils.CsvManager.getReleasesIndexes;
import static org.example.utils.CsvManager.writeModelPerformances;
import static org.example.utils.GithubRepoUtilities.getRepo;

public class MainClass {
    private static final String PROJECT_NAME = "TAJO";
    public static String getProjName(){
        return PROJECT_NAME;
    }

    public static void main(String[] args) throws Exception {

        List<Ticket> ticketList = getTickets(PROJECT_NAME);
        assignIV(ticketList);
        List<Ticket> validTickets= RetrieveTickets.selectValidTickets(ticketList);

      List<Commit> cmts;
        Set<Commit> finalcommits=new HashSet<>();
        Set<RevCommit> commits= GithubRepoUtilities.getGithubCommits();

        for(Ticket tkt: validTickets){
            //inserisco tutti i commits relativi ai tickets validi in una lista
            cmts=MergeTicketsAndCommits.createCommitList(tkt,commits);
            finalcommits.addAll(cmts);
        }

        Repository repo= getRepo();

        List<String[]> javaClassesTot= new ArrayList<>();
        List<Commit> commitList= new ArrayList<>();

        addCommitsOfFirstRelease(commits, finalcommits);
        for(Commit commit: finalcommits){
            javaClassesTot.addAll(JavaFiles.retrieveJavaClasses(commit.getRevCommit(), repo));
        }


        Map<String,Commit> modifiedClasses= new HashMap<>();

        for(Commit commit: finalcommits){
            if(commit.getAssociatedTicket()!=null){
                modifiedClasses.putAll(JavaFiles.getModifiedClasses(commit, repo));
                commitList.add(commit);}

        }
       List<String> releasesIndexes=getReleasesIndexes(PROJECT_NAME+"VersionInfo.csv");
        writeModelPerformances(evaluateWalkForward(releasesIndexes, validTickets));
    }
}
