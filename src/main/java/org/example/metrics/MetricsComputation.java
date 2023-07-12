package org.example.metrics;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.example.Commit;
import org.example.JavaClass;
import org.example.utils.CsvManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.MainClass.getProjName;


public class MetricsComputation {
private MetricsComputation(){}
    public static int publicMethodsCounter(RawText rawText) {
        int count = 0;
        String pattern = "public+(\\sfinal\\s+\\s|static\\s+|\\s)?\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{"; //public +valori ritono +[static/final]+ nome metodo + parentesi con argomenti
        for (int j = 0; j < rawText.size(); j++) {

            Matcher matcher = Pattern.compile(pattern).matcher(rawText.getString(j));
            if (matcher.find()) {

                count++;
            }
        }
        return count;
    }

    public static int numberOfComments(RawText rawText) {

        int commentLines = 0;
        boolean inBlockComment = false;
        for (int j = 0; j < rawText.size(); j++) {
            String line = rawText.getString(j);
            line = line.trim();

            if (line.startsWith("/*")) {
                inBlockComment = true;
                commentLines++;
            } else if (inBlockComment) {
                commentLines++;
                if (line.endsWith("*/")) {
                    inBlockComment = false;
                }
            } else if (line.startsWith("//")) {
                commentLines++;
            }

        }
        return commentLines;
    }

    public static int countAuthorsFromReleaseZero(JavaClass j, List<JavaClass> javaClasses) {

        Set<String> authorsNames = new HashSet<>();
        List<Commit> commitList = new ArrayList<>();
        for (JavaClass javaClass : javaClasses) {
            if (javaClass.getPath().compareTo(j.getPath()) == 0 && javaClass.getRelease().compareTo(j.getRelease()) <= 0) {
                commitList.add(javaClass.getAssociatedCommit());
            }
        }
        for (Commit c : commitList) {

            authorsNames.add(c.getRevCommit().getAuthorIdent().getName());

        }


        return authorsNames.size();
    }

    public static int computeNR(JavaClass j, List<JavaClass> javaClasses) {
        int nr = 0;
        for (JavaClass javaClass : javaClasses) {
            if (javaClass.getPath().compareTo(j.getPath()) == 0 && javaClass.getRelease().compareTo(j.getRelease()) <= 0) {
                nr++;
            }
        }
        return nr;
    }

    public static long computeReleaseAge(JavaClass j) throws FileNotFoundException {
        String dateS = CsvManager.readCsvEntry(getProjName() + "VersionInfo.csv", 3, 0, String.valueOf(j.getRelease()));
        long age;

        DateTimeFormatter dateF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dateTime = LocalDate.parse(dateS.substring(0, 10), dateF);
        age = ChronoUnit.MONTHS.between(dateTime, LocalDate.now());

        return age;

    }


    public static int changeSetSize(JavaClass j, Repository repository){
        RevCommit commit= j.getAssociatedCommit().getRevCommit();
        int javaFileCount=0;
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(commit.getTree());
            treeWalk.addTree(commit.getParent(0).getTree());
            treeWalk.setRecursive(true);

            List<DiffEntry> diffs = DiffEntry.scan(treeWalk);
            for (DiffEntry entry : diffs) {
                // contare solo i file Java aggiunti/modificati/rimossi
                if (entry.getNewPath().endsWith(".java") && entry.getChangeType() != DiffEntry.ChangeType.DELETE) {
                    javaFileCount++;
                }
            }

        } catch (IOException e) {
           e.printStackTrace();}
       return javaFileCount;
    }



    public static int calculateChurn(List<Commit> commits, Repository repository, JavaClass javaClass) throws IOException {

        String classPath = javaClass.getPath();

        int churn = 0;


        // Calcola il churn sommando le righe aggiunte e rimosse
        for (Commit commit : commits) {

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(repository);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);

            // Ottiene il diff tra il commit corrente e il suo genitore
            RevCommit parent = commit.getRevCommit().getParent(0);
            List<DiffEntry> diffs = diffFormatter.scan(parent, commit.getRevCommit());

            for (DiffEntry diffEntry : diffs) {
                if (diffEntry.getNewPath().contains(classPath)) {

                    RevTree tree = commit.getRevCommit().getTree();
                    String oldPath=diffEntry.getOldPath();
                    String newPath= diffEntry.getNewPath();

                    ObjectReader reader=repository.newObjectReader();
                    RevCommit parentCommit = commit.getRevCommit().getParent(0);
                    RevTree parentTree = parentCommit.getTree();

                    TreeWalk treeWalk = TreeWalk.forPath(reader, oldPath, parentTree);
                    if(treeWalk!=null){
                        ObjectId oldObjectId = treeWalk.getObjectId(0);

                        treeWalk = TreeWalk.forPath(reader, newPath, tree);
                        ObjectId newObjectId = treeWalk.getObjectId(0);

                        ObjectLoader oldLoader = reader.open(oldObjectId);
                        byte[] oldBytes = oldLoader.getBytes();

                        ObjectLoader newLoader = reader.open(newObjectId);
                        byte[] newBytes = newLoader.getBytes();
                        churn=getLinesAdded(newBytes,oldBytes)+getLinesDeleted(newBytes,oldBytes);
                    }
                }
            }
        }

        return churn;
    }

    private static int getLinesAdded(byte[] newBytes, byte[] oldBytes) throws IOException {
        String oldContent = new String(oldBytes);
        String newContent = new String(newBytes);

        LineNumberReader oldReader = new LineNumberReader(new StringReader(oldContent));
        LineNumberReader newReader = new LineNumberReader(new StringReader(newContent));

        String oldLine = oldReader.readLine();
        String newLine = newReader.readLine();

        int addedLines = 0;

        while (oldLine != null && newLine != null) {
            if (oldLine.equals(newLine)) {
                oldLine = oldReader.readLine();
                newLine = newReader.readLine();

            }
             while (newLine != null && !newLine.equals(oldLine)) {
                addedLines++;
                newLine = newReader.readLine();
            }

    }

        while (newLine != null) {
            addedLines++;
            newLine = newReader.readLine();
        }

        return addedLines ;
    }
    private static int getLinesDeleted(byte[] newBytes, byte[] oldBytes) throws IOException {
        String oldContent = new String(oldBytes);
        String newContent = new String(newBytes);

        LineNumberReader oldReader = new LineNumberReader(new StringReader(oldContent));
        LineNumberReader newReader = new LineNumberReader(new StringReader(newContent));

        String oldLine = oldReader.readLine();
        String newLine = newReader.readLine();
        int deletedLines=0;

        while (oldLine != null && newLine != null) {
            if (oldLine.equals(newLine)) {
                oldLine = oldReader.readLine();
                newLine = newReader.readLine();
                continue;
            }


            while (oldLine != null && !oldLine.equals(newLine)) {
                deletedLines++;
                oldLine = oldReader.readLine();
            }




    }
        while(oldLine!=null){
            deletedLines++;
            oldLine=newReader.readLine();
        }
        return deletedLines;

}

    public static void assignMetrics(List<JavaClass> javaClassesList, Repository repo, Map<String,Commit> modifiedClasses, List<Commit> commitList) throws IOException {
        for(JavaClass j: javaClassesList){

            j.setChangeSetSize(MetricsComputation.changeSetSize(j,repo));
            j.setAuthNum(MetricsComputation.countAuthorsFromReleaseZero(j,javaClassesList));
            j.setNR(MetricsComputation.computeNR(j, javaClassesList));
            j.setAge(MetricsComputation.computeReleaseAge(j));
            for(String modClass: modifiedClasses.keySet()){
                if(j.getPath().compareTo(modClass)==0){

                    j.setChurn(MetricsComputation.calculateChurn(commitList,repo,j));
                }
            }

        }
    }
}









