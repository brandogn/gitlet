package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static gitlet.Utils.readContentsAsString;

/**
 * Various Utils that are used when executing commands:
 * - saving and retrieving repo info
 * - deserializing gitlet objects from the .gitlet directory structure
 * - Creating and serializing objects into .gitlet directory structure
 * - retrieving current status of misc conditions
 * - printing helper functions
 *
 * @author Brandon Wong
 */
public class RepoUtils implements Serializable {

    // ==================== Save/Get Repo Info Methods: ====================

    public static void saveHeadCommitHash(String headCommitHash) {
        writeContents(HEAD_COMMIT_FILE, headCommitHash);
    }

    public static void saveHeadBranchName(String headBranchName) {
        writeContents(HEAD_BRANCH_FILE, headBranchName);
    }

    public static String getHeadCommitHash() {
        return readContentsAsString(HEAD_COMMIT_FILE);
    }

    public static String getHeadBranchName() {
        return readContentsAsString(HEAD_BRANCH_FILE);
    }

    public static Commit getHeadCommit() {
        File locationOfHeadCommit = join(COMMITS_DIR, getHeadCommitHash());
        return readObject(locationOfHeadCommit, Commit.class);
    }

    public static Branch getHeadBranch() {
        File locationOfHeadBranch = join(BRANCHES_DIR, getHeadBranchName());
        return readObject(locationOfHeadBranch, Branch.class);
    }

    // ==================== Getting Methods: ====================

    /**
     * Returns Commit object from commits folder based on provided
     * commitHash, returns null if commit with given hash does not exist
     */
    public static Commit getCommitFromHash(String commitHash) {
        File location = join(COMMITS_DIR, commitHash);
        if (location.exists()) {
            return readObject(location, Commit.class);
        } else {
            return null;
        }
    }

    /**
     * Retrieves branch based on given branch name.
     * If branch doesn't exist in file structure, returns null.
     *
     * @param branchName - String, name of branch
     * @return - branch object from file named branchName
     */
    public static Branch getBranchFromName(String branchName) {
        File branch = join(BRANCHES_DIR, branchName);
        if (branch.exists()) {
            return readObject(branch, Branch.class);
        } else {
            return null;
        }
    }

    /**
     * Returns all files in cwd that are untracked by given commit
     *
     * @param commit - Given commit object
     * @return - ArrayList of all untracked files
     */
    public static ArrayList<String> getUntrackedFiles(Commit commit) {
        ArrayList<String> untracked = new ArrayList<>();
        for (String elem : plainFilenamesIn(CWD)) {
            if (!isStaged(elem) && !commit.contains(elem)) {
                untracked.add(elem);
            }
        }
        return untracked;
    }

    /**
     * Returns all files in cwd that differ from their versions in given commit
     * Tags them with respective tags: modified/deleted
     *
     * @param commit - Given commit object
     * @return - ArrayList of all modded files with respective tags
     */
    public static ArrayList<String> getModsNotStaged(Commit commit) {
        ArrayList<String> modsNotStaged = new ArrayList<>();
        for (String elem : commit.getSnapShots().keySet()) {
            if (!isStaged(elem) && !commit.contains(elem)) {
                if (join(CWD, elem).exists()) {
                    modsNotStaged.add(elem + "(modified)");
                } else {
                    modsNotStaged.add(elem + "(deleted)");
                }
            }
        }
        return modsNotStaged;
    }

    public static Commit getSplitCommit(String b1Name, String b2Name) {
        // Note: this split command is not very smart
        // It will only search the graph 1 parent away from
        // main branch each time (doesn't fully search)
        Branch b1 = getBranchFromName(b1Name),
                b2 = getBranchFromName(b2Name);
        Commit b1ptr = b1.getFrontCommit(),
                b2ptr = b2.getFrontCommit();
        List<String> b1Hashes = new ArrayList<>(),
                b2Hashes = new ArrayList<>();
        b1Hashes.add(b1ptr.getCommitHash());
        b2Hashes.add(b2ptr.getCommitHash());

        while (b1ptr.hasParent()) {
            b1ptr = getCommitFromHash(b1ptr.getParentHash());
            b1Hashes.add(b1ptr.getCommitHash());
        }
        while (b2ptr.hasParent()) {
            b2Hashes.add(b2ptr.getCommitHash());
            if (b1Hashes.contains(b2ptr.getCommitHash())) {
                return b2ptr;
            }
            // bruh
            String b2ptrMPHash = b2ptr.getMergedParentHash();
            if (b2ptrMPHash != null) {
                b2Hashes.add(b2ptrMPHash);
                if (b1Hashes.contains(b2ptrMPHash)) {
                    return getCommitFromHash(b2ptrMPHash);
                }
            }
            // end of bruh
            b2ptr = getCommitFromHash(b2ptr.getParentHash());
        }
        return b1ptr;
    }

    // ==================== Creating/Modding Methods: ====================

    /**
     * creates new commit in file structure and returns its hash
     *
     * @param message - message of commit
     * @return - String representing the hash of newly created commit
     */
    public static String createNewCommit(String message, HashMap<String, String> copies) {
        Commit newCommit = new Commit(message, copies);
        newCommit.save();
        return newCommit.getCommitHash();
    }

    /**
     * creates new branch in file structure and returns branch name
     *
     * @param name            - name of branch
     * @param frontCommitHash - hash of front commit
     * @return - name of branch
     */
    public static String createNewBranch(String name, String frontCommitHash) {
        Branch newBranch = new Branch(name, frontCommitHash);
        newBranch.save();
        return name;
    }

    /**
     * Stages file from cwd (based on given fileName) for addition,
     * serializes a new snapShot object into staging area for adds
     */
    public static void stageForAdd(String fileName) {
        File fileToStageAdd = join(STAGING_ADD_DIR, fileName);
        String cwdContent = readContentsAsString(Utils.join(CWD, fileName));
        writeContents(fileToStageAdd, cwdContent);
    }

    /**
     * Stages file from cwd (based on given fileName) for removal,
     * adds a placeholder file with fileName into staging area for removes
     */
    public static void stageForRm(String fileName) {
        File fileToStageRm = join(STAGING_RM_DIR, fileName);
        writeContents(fileToStageRm, "");
    }

    /**
     * Moves snapShot from staging area to snaps directory.
     * Returns array of Strings formatted as
     * {snapShotName, snapShotHash}
     */
    public static String[] moveSnapFromStage(String fileName) {
        File stagePath = join(STAGING_ADD_DIR, fileName);

        String newContent = readContentsAsString(stagePath);
        SnapShot stagedSnap = new SnapShot(fileName, newContent, 1);
        stagedSnap.save();
//        SnapShot stagedSnap = getSnapShotFromFile(stagePath);
//        stagedSnap.saveTo(SNAPS_DIR);
        stagePath.delete();
        return new String[]{stagedSnap.getName(), stagedSnap.getHash()};
    }

    public static String mergeFiles(String fileName,
                                    Commit curr,
                                    Commit given) {
        String res = """
                <<<<<<< HEAD
                ${CURR CONTENTS}=======
                ${GIVEN CONTENTS}>>>>>>>
                """;
        String currContents = curr.getContentOfFile(fileName),
                givenContents = given.getContentOfFile(fileName);
        return res.replace("${CURR CONTENTS}", currContents)
                .replace("${GIVEN CONTENTS}", givenContents);
    }

    // ==================== Misc Methods: ====================

    public static boolean branchExists(String name) {
        return plainFilenamesIn(BRANCHES_DIR).contains(name);
    }

    public static boolean commitExists(String name) {
        return plainFilenamesIn(COMMITS_DIR).contains(name);
    }

    public static boolean stagedChangesExist() {
        List<String> stagedFilesToAdd = plainFilenamesIn(STAGING_ADD_DIR);
        List<String> stagedFilesToRm = plainFilenamesIn(STAGING_RM_DIR);
        return stagedFilesToAdd.size() != 0 || stagedFilesToRm.size() != 0;
    }

    public static boolean isStaged(String fileName) {
        return plainFilenamesIn(STAGING_ADD_DIR).contains(fileName)
                || plainFilenamesIn(STAGING_RM_DIR).contains(fileName);
    }

    public static boolean isSame(String key,
                                 HashMap<String, String> firstMap,
                                 HashMap<String, String> secondMap) {
        String firstMapVal = firstMap.get(key),
                secondMapVal = secondMap.get(key);
        return (firstMapVal != null
                && secondMapVal != null
                && firstMapVal.equals(secondMapVal))
                ||
                (firstMapVal == null
                        && secondMapVal == null);
    }

    // ==================== Printing Methods: ====================

    /**
     * For status command. Prints the header in a seperate line, then each
     * element of given list on their seperate lines, then an empty line.
     */
    public static void printList(String header, List<String> list) {
        System.out.println(header);
        for (String elem : list) {
            System.out.println(elem);
        }
        System.out.println();
    }

    /**
     * Recursively prints each commit going back until it reaches a commit with no parent.
     */
    public static void printAllCommits(Commit c) {
        System.out.println(c.toLogString());
        if (c.getParentHash() == null) {
            return;
        }
        Commit parentCommit = getCommitFromHash(c.getParentHash());
        printAllCommits(parentCommit);
    }

}
