package org.example;

import org.eclipse.jgit.revwalk.RevCommit;
import org.example.jira_tickets.Ticket;

public class Commit {
    private String id;

    private String release;
    private RevCommit revCommit;
    private Ticket associatedTicket;

    public Ticket getAssociatedTicket() {
        return associatedTicket;
    }

    public void setAssociatedTicket(Ticket associatedTicket) {
        this.associatedTicket = associatedTicket;
    }

    public RevCommit getRevCommit() {
        return revCommit;
    }

    public void setRevCommit(RevCommit revCommit) {
        this.revCommit = revCommit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }


}
