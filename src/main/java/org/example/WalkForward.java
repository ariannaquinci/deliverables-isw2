package org.example;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.example.jira_tickets.Ticket;
import org.example.metrics.MetricsComputation;
import org.example.utils.CsvManager;
import org.example.utils.GithubRepoUtilities;
import weka.core.converters.ConverterUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.example.MainClass.getProjName;
import static org.example.MergeTicketsAndCommits.addCommitsOfFirstRelease;
import static org.example.utils.GithubRepoUtilities.getRepo;

public class WalkForward {
    private WalkForward(){
        //void private constructor to hyde the public one
    }
    private static void createTicketCommitList(Ticket tkt, int i, Set<RevCommit> commits,Set<Commit> finalCommits ){
        List<Commit> cmts;
        if (tkt.getFV().compareTo(String.valueOf(i)) <= 0) {
            cmts = MergeTicketsAndCommits.createCommitList(tkt, commits);
            //inserisco tutti i commits relativi ai tickets validi in un hashset
            finalCommits.addAll(cmts);
        }

    }
    public static ConverterUtils.DataSource buildTrainingSet(List<Ticket> ticketList, int i) throws Exception {

        //I use walk forward as valutation technique
        Repository repo = getRepo();
        Set<RevCommit> commits;



        //The number of iterations of walk forward is given by the number of the releases I consider

        Set<Commit> finalCommits = new HashSet<>();
        commits = GithubRepoUtilities.getGithubCommits();

        for (Ticket tkt : ticketList) {
            //creo la lista dei commits per tkt
            createTicketCommitList(tkt,i, commits, finalCommits);

        }
        List<String[]> javaClassesTot = new ArrayList<>();
        List<Commit> commitList = new ArrayList<>();
        //aggiungo i commits relativi alla prima release per prendere le classi java alla prima release,
        // tranne nella prima iterazione in cui walk forward ha come training set solo la prima release
        if (i != 0) {
            addCommitsOfFirstRelease(commits, finalCommits);
        }
        for (Commit commit : finalCommits) {
            //retrieve classi java a partire dai commits nell'hashset finalCommits
            javaClassesTot.addAll(JavaFiles.retrieveJavaClasses(commit.getRevCommit(), repo));
        }


        List<JavaClass> javaClassesList = JavaFiles.createJavaClasses(javaClassesTot, finalCommits);


        HashMap<String, Commit> modifiedClasses = new HashMap<>();

        for (Commit commit : finalCommits) {
            if (commit.getAssociatedTicket() != null) {
                modifiedClasses.putAll(JavaFiles.getModifiedClasses(commit, repo));
                commitList.add(commit);
            }
        }


        JavaFiles.assignRelease(javaClassesList);
        JavaFiles.deleteDuplicates(javaClassesList);

        JavaFiles.setBuggyTrainingSet(javaClassesList, modifiedClasses);
        for (JavaClass jClass : javaClassesList) {
            jClass.setChangeSetSize(MetricsComputation.changeSetSize(jClass, repo));
            jClass.setAuthNum(MetricsComputation.countAuthorsFromReleaseZero(jClass, javaClassesList));
            jClass.setNR(MetricsComputation.computeNR(jClass, javaClassesList));
            jClass.setAge(MetricsComputation.computeReleaseAge(jClass));
            for (String modClass : modifiedClasses.keySet()) {
                if (jClass.getPath().compareTo(modClass) == 0) {

                    jClass.setChurn(MetricsComputation.calculateChurn(commitList, repo, jClass));
                }
            }

        }
        FileWriter fw = new FileWriter(getProjName() + i + "TRAINING.csv");
        javaClassesList.sort((j1,j2)->Boolean.compare(j1.isBuggy(), j2.isBuggy()));
        CsvManager.writeCSVMetrics(javaClassesList, fw);

        return  new ConverterUtils.DataSource(CsvManager.convertCSVtoarff(getProjName() + i + "TRAINING.csv", i, "TRAINING"));


}

    public static ConverterUtils.DataSource buildTestingSet(List<Ticket> tickets, int i) throws Exception {
        Repository repo = getRepo();
        Set<RevCommit> commits = GithubRepoUtilities.getGithubCommits();
        List<String[]> javaClassesTot = new ArrayList<>();
        List<Commit> commitList = new ArrayList<>();
        HashSet<Commit> totalcommits = new HashSet<>();


        for (Ticket tkt : tickets) {
            List<Commit> cmts = MergeTicketsAndCommits.createCommitList(tkt, commits);
            totalcommits.addAll(cmts);

        }

        addCommitsOfFirstRelease(commits, totalcommits);
        for (Commit commit : totalcommits){
            javaClassesTot.addAll(JavaFiles.retrieveJavaClasses(commit.getRevCommit(), repo));
        }

        List<JavaClass> javaClassesList = JavaFiles.createJavaClasses(javaClassesTot, totalcommits);
        JavaFiles.deleteDuplicates(javaClassesList);
        List<JavaClass> finalJavaClassesList = new ArrayList<>();

        for (JavaClass javaClass : javaClassesList) {
            if (Integer.parseInt(javaClass.getRelease()) == i) {
                finalJavaClassesList.add(javaClass);
            }
        }

        HashMap<String, Commit> modifiedClasses = new HashMap<>();
        for (Commit commit : totalcommits) {
            if (commit.getAssociatedTicket() != null) {
                modifiedClasses.putAll(JavaFiles.getModifiedClasses(commit, repo));
                commitList.add(commit);
            }
        }

        JavaFiles.setBuggyTestingSet(finalJavaClassesList, modifiedClasses);

        for (JavaClass jClass : finalJavaClassesList) {
           setMetrics(jClass, repo,javaClassesList,modifiedClasses,commitList);
        }
        FileWriter fw = new FileWriter(getProjName() + i + "TESTING.csv");
        finalJavaClassesList.sort((j1,j2)->Boolean.compare(j1.isBuggy(), j2.isBuggy()));
        CsvManager.writeCSVMetrics(finalJavaClassesList, fw);
        return  new ConverterUtils.DataSource(
                CsvManager.convertCSVtoarff(getProjName() + i + "TESTING.csv", i, "TESTING")
        );


    }
    private static void setMetrics(JavaClass jClass, Repository repo, List<JavaClass> javaClassesList, HashMap<String,Commit> modifiedClasses, List<Commit> commitList) throws  IOException {
        if (jClass.isBuggy()) {
            jClass.setChangeSetSize(MetricsComputation.changeSetSize(jClass, repo));
            jClass.setAuthNum(MetricsComputation.countAuthorsFromReleaseZero(jClass,javaClassesList));
            jClass.setNR(MetricsComputation.computeNR(jClass, javaClassesList));
            jClass.setAge(MetricsComputation.computeReleaseAge(jClass));

            for (String modClass : modifiedClasses.keySet()) {
                if (jClass.getPath().compareTo(modClass) == 0) {
                    jClass.setChurn(MetricsComputation.calculateChurn(commitList, repo, jClass));
                }
            }
        }
    }
}