package gitlet;

import java.io.File;
import java.io.Serializable;

/**
 * Represents a gitlet snapshot object. This object represents a snapshot
 * of a file, containing the name, content of the file, and (version?).
 *
 * @author Brandon Wong
 */
public class SnapShot implements Serializable {

    // ==================== SnapShot Metadata: ====================
    private final String name;
    private final String content;
    private final String hash;
    private int version;

    /**
     * Initiates a snapshot of given filename,
     * with given string content, with given version
     */
    public SnapShot(String name, String content, int version) {
        this.name = name;
        this.content = content;
        this.version = version;
        this.hash = Utils.sha1(name, content);
    }

    // ==================== Getters and Setters: ====================

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getHash() {
        return hash;
    }

    // ==================== Misc Methods: ====================

    public void save() {
        File thisSnapFile = Utils.join(Repository.SNAPS_DIR, hash);
        Utils.writeObject(thisSnapFile, this);
    }

}
