package org.example.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class GithubRepoUtilities {


        //private static String homeDirectory = System.getProperty("user.home");
        private static Path localPath = Paths.get("../bookkeeper");


        private static Git git;

        static {
            try {
                git = Git.open(localPath.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        public static Repository getRepo() {

            Repository repo = git.getRepository();
            return repo;
        }

        public static Set<RevCommit> getGithubCommits() throws Exception {
            Set<RevCommit> allCommitsList = new HashSet<>();
            Iterable<Ref> refs = git.branchList().call();
            for (Ref ref : refs) {
                // Ottiene i commit per ogni riferimento
                LogCommand log = git.log().add(ref.getObjectId());
                Iterable<RevCommit> commits = log.call();
                for (RevCommit commit : commits) {
                    allCommitsList.add(commit);
                }

            }
            return allCommitsList;

        }
    }



