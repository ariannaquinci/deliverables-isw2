package org.example.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.example.MainClass;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class GithubRepoUtilities {
        private GithubRepoUtilities(){
            //private constructor to hyde the public one
        }

        private static Path localPath = Paths.get("../"+ MainClass.getProjName().toLowerCase());


        private static Git git;

        static {
            try {
                git = Git.open(localPath.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public static Repository getRepo() {

            return git.getRepository();

        }

        public static Set<RevCommit> getGithubCommits() {
            Set<RevCommit> allCommitsList = new HashSet<>();

            Iterable<Ref> refs = null;
            try {
                refs = git.branchList().call();
            } catch (GitAPIException e) {
               e.printStackTrace();
            }
            for (Ref ref : refs) {
                // Ottiene i commit per ogni riferimento
                LogCommand log = null;
                try {
                    log = git.log().add(ref.getObjectId());
                } catch (MissingObjectException |IncorrectObjectTypeException e) {
                    e.printStackTrace();
                }
                Iterable<RevCommit> commits = null;
                try {
                    commits = log.call();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
                for (RevCommit commit : commits) {
                    allCommitsList.add(commit);
                }

            }
            return allCommitsList;

        }
    }



