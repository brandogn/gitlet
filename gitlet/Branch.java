package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Represents a gitlet branch object. (name and hash of front commit)
 *
 * @author Brandon Wong
 */
public class Branch implements Serializable {

    // ==================== Branch Metadata: ====================
    private final String name;
    private String frontCommitHash;

    /**
     * Instantiates a new Branch object with given name and front commit hash.
     */
    Branch(String name, String frontCommitHash) {
        this.name = name;
        this.frontCommitHash = frontCommitHash;
    }

    // ==================== Getters and Setters: ====================

    public String getFrontCommitHash() {
        return frontCommitHash;
    }

    public void setFrontCommitHash(String frontCommitHash) {
        this.frontCommitHash = frontCommitHash;
    }

    public Commit getFrontCommit() {
        return RepoUtils.getCommitFromHash(frontCommitHash);
    }

    // ==================== Misc Methods: ====================

    /**
     * Saves branch in Branch directory as file "{branch name}"
     */
    public void save() {
        File thisCommitFile = Utils.join(Repository.BRANCHES_DIR, name);
        Utils.writeObject(thisCommitFile, this);
    }

    /**
     * Moves branches pointer to given Hash, serializes changes.
     */
    public void moveFrontTo(String newHash) {
        setFrontCommitHash(newHash);
        this.save();
    }

    /**
     * Adds a new commit with given message & hashmap of snapshots to front of branch
     * Returns the hash of the newly created commit
     */
    public String addNewCommitWith(String message, HashMap<String, String> snapShots) {
        Commit newCommit = new Commit(message, snapShots);
        newCommit.setParentHash(frontCommitHash);
        this.setFrontCommitHash(newCommit.getCommitHash());

        newCommit.save();
        this.save();

        return newCommit.getCommitHash();
    }

}
