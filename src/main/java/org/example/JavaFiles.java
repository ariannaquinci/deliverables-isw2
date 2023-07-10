package org.example;


import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.example.metrics.MetricsComputation;

import java.io.IOException;
import java.util.*;

public class JavaFiles {
    private JavaFiles() {
        //private constructor to hyde the default public one
    }

    public static List<String[]> retrieveJavaClasses(RevCommit commit, Repository repository) throws IOException{


        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"commit_id", "java_files", "size"});
        List<String> touchedClasses = new ArrayList<>();

        int i = 0;
        try (RevWalk walk = new RevWalk(repository)) {

            RevTree tree = walk.parseTree(commit.getTree().getId());
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    if (treeWalk.getPathString().endsWith(".java") && !treeWalk.getPathString().contains("/test") ) { //considero solo i file java che non sono classi di test
                        ObjectId objectId = treeWalk.getObjectId(0);
                        ObjectLoader loader = repository.open(objectId);
                        touchedClasses.add(treeWalk.getPathString());
                        RawText rawText = new RawText(loader.getBytes());

                        rawText.getLineDelimiter();


                        data.add(new String[]{touchedClasses.get(i), commit.getId().toString(), String.valueOf(rawText.size()), String.valueOf(MetricsComputation.publicMethodsCounter(rawText)), String.valueOf(MetricsComputation.numberOfCommits(rawText))});

                        i++;
                    }
                }
            }
        }
        return data;

    }


    public static List<JavaClass> createJavaClasses(List<String[]> javaClassesString, Set<Commit> finalCommits) {
        List<JavaClass> jclasses = new ArrayList<>();
        int i = 0;
        for (Commit cmt : finalCommits) {
            for (String[] s : javaClassesString) {
                if (s[1].compareTo(cmt.getId()) == 0) {
                    JavaClass javaclass = new JavaClass();
                    javaclass.setIndex(i++);
                    javaclass.setPath(s[0]);
                    javaclass.setRelease(cmt.getRelease());
                    javaclass.setSize(Integer.parseInt(s[2]));
                    javaclass.setAssociatedCommit(cmt);
                    javaclass.setPublicMethodsCounter(Integer.parseInt(s[3]));
                    javaclass.setCommentCounter(Integer.parseInt(s[4]));
                    jclasses.add(javaclass);
                }


            }
        }
        return jclasses;
    }


    public static void deleteDuplicates(List<JavaClass> javaClassList) {


        Set<String> seenPathsAndReleases = new HashSet<>(); // inizializza un HashSet vuoto

        Iterator<JavaClass> iterator = javaClassList.iterator(); // ottieni un iterator per la lista

        while (iterator.hasNext()) {
            JavaClass javaClass = iterator.next();
            String pathAndRelease = javaClass.getPath() + javaClass.getRelease(); // unisci il path e la release

            if (seenPathsAndReleases.contains(pathAndRelease)) {
                iterator.remove(); // rimuovi l'oggetto dalla lista
            } else {
                seenPathsAndReleases.add(pathAndRelease); // aggiungi il path e la release all'HashSet
            }
        }
        Collections.sort(javaClassList, (j1, j2) -> j1.getRelease().compareTo(j2.getRelease()));
    }



    public static Map<String,Commit> getModifiedClasses(Commit cmt, Repository repo) throws IOException {
        //con questo metodo ottengo solo le classi java modificate da un commit
        Map<String,Commit> modifiedClasses = new HashMap<>();
        RevCommit commit= cmt.getRevCommit();

        try(DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            ObjectReader reader = repo.newObjectReader()) {

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            ObjectId newTree = commit.getTree();
            newTreeIter.reset(reader, newTree);

            RevCommit commitParent = commit.getParent(0);
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            ObjectId oldTree = commitParent.getTree();
            oldTreeIter.reset(reader, oldTree);

            diffFormatter.setRepository(repo);
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);

            for(DiffEntry entry : entries) {


                if(entry.getChangeType().equals(DiffEntry.ChangeType.MODIFY) && entry.getNewPath().contains(".java") && !entry.getNewPath().contains("/test")) {
                    modifiedClasses.put(entry.getNewPath(), cmt);


               }
            }

        } catch(ArrayIndexOutOfBoundsException e) {
            //commit has no parents: skip this commit

        }

        return modifiedClasses;

    }

    public static void assignRelease(List<JavaClass> jClasses){

        for(JavaClass j: jClasses) {
            if(j.getAssociatedCommit().getAssociatedTicket()!=null){
            j.setRelease(j.getAssociatedCommit().getAssociatedTicket().getFV());}
            else{j.getAssociatedCommit().getRelease();}


        }

    }


    public static void setBuggy(List<JavaClass> javaClasses, Map<String, Commit> modClasses) {
        List<JavaClass> jList = new ArrayList<>();

        for (JavaClass j : javaClasses) {
            boolean isBuggy = false;

            for (Map.Entry<String, Commit> entry : modClasses.entrySet()) {

                    if (j.getPath().compareTo(entry.getKey()) == 0 &&
                            entry.getValue().getAssociatedTicket().getIV().compareTo(j.getRelease()) <= 0) {
                        isBuggy = true;
                        break;
                    }

            }

            j.setBuggy(isBuggy);
            if (isBuggy) {
                jList.add(j);
            }
        }
    }






}


