package org.example;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.example.jira_tickets.Ticket;
import org.example.metrics.MetricsComputation;
import org.example.utils.CsvManager;
import org.example.utils.GithubRepoUtilities;
import weka.core.converters.ConverterUtils;

import java.io.FileWriter;
import java.util.*;

import static org.example.MainClass.getProjName;
import static org.example.MergeTicketsAndCommits.addCommitsOfFirstRelease;
import static org.example.utils.GithubRepoUtilities.getRepo;

public class WalkForward {
    public static ConverterUtils.DataSource buildTRSET(List<Ticket> ticketList, int i) throws Exception {

        //I use walk forward as valutation technique
        Repository repo = getRepo();
        Set<RevCommit> commits;

        //     FileWriter f= new FileWriter("BuggyClassesNumber");

        //The number of iterations of walk forward is given by the number of the releases I consider
        List<Commit> cmts;
        HashSet<Commit> finalcommits = new HashSet<>();
        commits = GithubRepoUtilities.getGithubCommits();

        for (Ticket tkt : ticketList) {
            //creo la lista dei commits per tkt
            if (tkt.getFV().compareTo(String.valueOf(i)) <= 0) {
                cmts = MergeTicketsAndCommits.createCommitList(tkt, commits);
                //inserisco tutti i commits relativi ai tickets validi in un hashset
                finalcommits.addAll(cmts);
            }
        }
        List<String[]> javaClassesTot = new ArrayList<>();
        List<Commit> commitList = new ArrayList<>();
        //aggiungo i commits relativi alla prima release per prendere le classi java alla prima release,
        // tranne nella prima iterazione in cui walk forward ha come training set solo la prima release
        if (i != 0) {
            addCommitsOfFirstRelease(commits, finalcommits);
        }
        for (Commit commit : finalcommits) {
            //retrieve classi java a partire dai commits nell'hashset finalCommits
            javaClassesTot.addAll(JavaFiles.retrieveJavaClasses(commit.getRevCommit(), repo));
        }


        List<JavaClass> javaClassesList = JavaFiles.createJavaClasses(javaClassesTot, finalcommits);


        HashMap<String, Commit> modifiedClasses = new HashMap<>();

        for (Commit commit : finalcommits) {
            if (commit.getAssociatedTicket() != null) {
                modifiedClasses.putAll(JavaFiles.getModifiedClasses(commit, repo));
                commitList.add(commit);
            }
        }


        JavaFiles.assignRelease(javaClassesList);
        JavaFiles.deleteDuplicates(javaClassesList);

        JavaFiles.setBuggy(javaClassesList, modifiedClasses);
        for (JavaClass jClass : javaClassesList) {
            jClass.setChangeSetSize(MetricsComputation.changeSetSize(jClass, repo));
            jClass.setAuthNum(MetricsComputation.countAuthorsFromReleaseZero(jClass, javaClassesList));
            jClass.setNR(MetricsComputation.computeNR(jClass, javaClassesList));
            jClass.setAge(MetricsComputation.computeReleaseAge(jClass, javaClassesList));
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

    public static ConverterUtils.DataSource buildTS(List<Ticket> tickets, int i) throws Exception {
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

        JavaFiles.setBuggy(finalJavaClassesList, modifiedClasses);

        for (JavaClass jClass : finalJavaClassesList) {
            if (jClass.isBuggy()) {
                jClass.setChangeSetSize(MetricsComputation.changeSetSize(jClass, repo));
                jClass.setAuthNum(MetricsComputation.countAuthorsFromReleaseZero(jClass,javaClassesList));
                jClass.setNR(MetricsComputation.computeNR(jClass, javaClassesList));
                jClass.setAge(MetricsComputation.computeReleaseAge(jClass, javaClassesList));

                for (String modClass : modifiedClasses.keySet()) {
                    if (jClass.getPath().compareTo(modClass) == 0) {
                        jClass.setChurn(MetricsComputation.calculateChurn(commitList, repo, jClass));
                    }
                }
            }
        }
        FileWriter fw = new FileWriter(getProjName() + i + "TESTING.csv");
        finalJavaClassesList.sort((j1,j2)->Boolean.compare(j1.isBuggy(), j2.isBuggy()));
        CsvManager.writeCSVMetrics(finalJavaClassesList, fw);
        return  new ConverterUtils.DataSource(
                CsvManager.convertCSVtoarff(getProjName() + i + "TESTING.csv", i, "TESTING")
        );


    }
}