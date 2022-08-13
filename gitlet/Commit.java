package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;

/**
 * Represents a gitlet commit object.
 *
 * @author Brandon Wong
 */
public class Commit implements Serializable {

    // ==================== Commit Metadata: ====================
    private final String message;
    private final String timeStamp;
    private final String commitHash;
    private String parentHash;
    private String mergedParentHash;
    private final HashMap<String, String> snapShots;

    // ==================== Constructors: ====================

    /**
     * Initializes a new Commit object with:
     *
     * @param message   - message of commit
     * @param snapShots - files it is pointing to
     */
    public Commit(String message, HashMap<String, String> snapShots) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        this.message = message;
        this.timeStamp = dateFormat.format(new Date());
        this.commitHash = Utils.sha1(message, timeStamp, String.valueOf(Math.random()));
        this.snapShots = snapShots;
        this.parentHash = null;
        this.mergedParentHash = null;
    }

    // ==================== Getters and Setters: ====================

    public void setParentHash(String pHash) {
        this.parentHash = pHash;
    }

    public String getParentHash() {
        return parentHash;
    }

    public void setMergedParentHash(String mergedParentHash) {
        this.mergedParentHash = mergedParentHash;
    }

    public String getMergedParentHash() {
        return mergedParentHash;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public HashMap<String, String> getSnapShots() {
        return snapShots;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasParent() {
        return parentHash != null || mergedParentHash != null;
    }

    // ==================== Misc Methods: ====================

    /**
     * Saves Commit in Commits directory as file "{commit sha1}"
     */
    public void save() {
        File thisCommitFile = Utils.join(Repository.COMMITS_DIR, commitHash);
        Utils.writeObject(thisCommitFile, this);
    }

    /**
     * Returns true if commit contains given fileName
     */
    public boolean contains(String fileName) {
        return snapShots.containsKey(fileName);
    }

    /**
     * returns true if provided content is modified in cwd
     * (file {name} is different from its version in the commit)
     * false if
     */
    public boolean isContentModded(String fileName) {
        String mappedVal = snapShots.get(fileName);
        if (mappedVal != null) {
            String commitContent = getContentOfFile(fileName);
            File cwdFile = Utils.join(Repository.CWD, fileName);
            String cwdContent = Utils.readContentsAsString(cwdFile);
            return !commitContent.equals(cwdContent);
        } else {
            return false;
        }
    }

    /**
     * Note: do not use unless you have verified that the given commit
     * contains file named {name} (use contains() method first)
     */
    public String getContentOfFile(String fileName) {
        String mappedVal = snapShots.get(fileName);
        if (mappedVal != null) {
            File location = Utils.join(Repository.SNAPS_DIR, mappedVal);
            SnapShot requestedSnap = Utils.readObject(location, SnapShot.class);
            return requestedSnap.getContent();
        } else {
            return "";
        }
    }

    // ==================== Printing: ====================

    public String toLogString() {
        String template = """
                ===
                ${HEAD}
                Date: ${DATE}
                ${MESSAGE}
                """;

        String header;
        if (mergedParentHash != null) {
            header = "commit " + commitHash + "\n"
                    + "Merge: " + parentHash.substring(0, 7) + " "
                    + mergedParentHash.substring(0, 7);
        } else {
            header = "commit " + commitHash;
        }

        return template
                .replace("${HEAD}", header)
                .replace("${DATE}", timeStamp)
                .replace("${MESSAGE}", message);
    }

    // ==================== Equals: ====================

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Commit)) {
            return false;
        }
        if (o == this) {
            return true;
        }

        return this.getCommitHash().equals(((Commit) o).getCommitHash());
    }

}
