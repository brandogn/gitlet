package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.RepoUtils.*;
import static gitlet.RepoUtils.commitExists;
import static gitlet.Utils.*;

/**
 * Represents a gitlet repository. Mainly exists to handle command
 * calls from Main and hold information about the head and stage.
 *
 * @author Brandon Wong
 */
public class Repository implements Serializable {

    // ==================== File Structure: ====================

    // cwd & .gitlet
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    // .gitlet folders
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    public static final File SNAPS_DIR = join(GITLET_DIR, "snaps");
    public static final File STAGING_ADD_DIR = join(GITLET_DIR, "staging", "adds");
    public static final File STAGING_RM_DIR = join(GITLET_DIR, "staging", "removes");
    // .gitlet files
    public static final File HEAD_COMMIT_FILE = join(GITLET_DIR, "HEAD.info");
    public static final File HEAD_BRANCH_FILE = join(GITLET_DIR, "BRANCH.info");

    // ==================== Command Functions: ====================

    /**
     * Initializes gitlet repository structure and create main branch
     * with initial commit with message "initial commit" and timestamp
     * 00:00:00 UTC, Thursday, 1 January 1970.
     *
     * @param args - formatted as {"init"}
     */
    public static void init(String[] args) {
        verifyOperandLen(args, 1);
        validate(!GITLET_DIR.exists(),
                "A Gitlet version-control system already exists in the current directory.");
        // persistence:
        GITLET_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        COMMITS_DIR.mkdir();
        SNAPS_DIR.mkdir();
        STAGING_ADD_DIR.mkdirs();
        STAGING_RM_DIR.mkdirs();
        // init commit + main branch
        String newCommitHash = createNewCommit("initial commit", new HashMap<>());
        String newBranchName = createNewBranch("main", newCommitHash);
        // save ^ to head
        saveHeadCommitHash(newCommitHash);
        saveHeadBranchName(newBranchName);
    }

    /**
     * Adds given file into the staging area only if it differs from the current
     * head's version of the file. If file is already staged, the newly added file should
     * override the old added file.
     *
     * @param args - formatted as {"add", fileName}
     */
    public static void add(String[] args) {
        verifyOperandLen(args, 2);
        String fileName = args[1];
        validate(join(CWD, fileName).exists(), "File does not exist.");

        Commit headCommit = getHeadCommit();

        join(STAGING_RM_DIR, fileName).delete();
        if (headCommit.contains(fileName) && !headCommit.isContentModded(fileName)) {
            join(STAGING_ADD_DIR, fileName).delete();
        } else {
            stageForAdd(fileName);
        }
    }

    /**
     * Creates a new commit in files, and add it to the current branch that the head is on.
     * Should also move head to this new commit
     *
     * @param args - formatted as {"commit", message}
     */
    public static void commit(String[] args) {
        verifyOperandLen(args, 2);
        String message = args[1];
        validate(stagedChangesExist(), "No changes added to the commit.");
        validate(message.length() > 0, "Please enter a commit message.");

        HashMap<String, String> newSnapShotsMap = getHeadCommit().getSnapShots();

        for (String elem : plainFilenamesIn(STAGING_ADD_DIR)) {
            // add or overwrite all commit files with staged files
            String[] elemInfo = moveSnapFromStage(elem);
            newSnapShotsMap.put(elemInfo[0], elemInfo[1]);
        }
        for (String elem : plainFilenamesIn(STAGING_RM_DIR)) {
            // apply all staged removes
            newSnapShotsMap.remove(elem);
            join(STAGING_RM_DIR, elem).delete();
        }

        // add new commit to head branch
        String newCommitHash = getHeadBranch().addNewCommitWith(args[1], newSnapShotsMap);
        saveHeadCommitHash(newCommitHash);
    }

    /**
     * Unstages File if currently staged
     * If file is tracked in curr commit, stage for removal and remove file from cwd
     *
     * @param args - formatted as {"rm", fileName}
     */
    public static void remove(String[] args) {
        verifyOperandLen(args, 2);
        String fileName = args[1];
        boolean existsInStaging = join(STAGING_ADD_DIR, fileName).delete();
        boolean existsInHeadCommit = getHeadCommit().contains(fileName);
        validate(existsInStaging || existsInHeadCommit, "No reason to remove the file.");

        if (existsInHeadCommit) {
            stageForRm(fileName);
            restrictedDelete(join(CWD, fileName));
        }
    }

    /**
     * Displays info on each commit backwards along commit tree.
     *
     * @param args - formatted as {"log"}
     */
    public static void log(String[] args) {
        verifyOperandLen(args, 1);
        printAllCommits(getHeadCommit());
    }

    /**
     * Displays information about all commits ever made (not in order)
     *
     * @param args - formatted as {"global-log"}
     */
    public static void globalLog(String[] args) {
        verifyOperandLen(args, 1);
        for (String elem : plainFilenamesIn(COMMITS_DIR)) {
            System.out.println(
                    readObject(join(COMMITS_DIR, elem), Commit.class).toLogString());
        }
    }

    /**
     * Prints all ids of commits that have given message, one per line
     *
     * @param args - formatted as {"find", commitMessage}
     */
    public static void find(String[] args) {
        verifyOperandLen(args, 2);
        boolean found = false;
        String findMessage = args[1];
        for (String elem : plainFilenamesIn(COMMITS_DIR)) {
            Commit elemCommit = readObject(join(COMMITS_DIR, elem), Commit.class);
            if (findMessage.equals(elemCommit.getMessage())) {
                System.out.println(elemCommit.getCommitHash());
                found = true;
            }
        }
        validate(found, "Found no commit with that message.");
    }

    /**
     * Displays what branches exist (marks curr branch with *), what files staged for add/rm
     *
     * @param args - formatted as  {"status"}
     */
    public static void status(String[] args) {
        verifyOperandLen(args, 1);

        Commit headCommit = getHeadCommit();
        List<String> branches = plainFilenamesIn(BRANCHES_DIR),
                adds = plainFilenamesIn(STAGING_ADD_DIR),
                removes = plainFilenamesIn(STAGING_RM_DIR),
                modsNotStaged = getModsNotStaged(headCommit),
                untracked = getUntrackedFiles(headCommit);

        String headBranchName = getHeadBranchName();
        branches.set(branches.indexOf(headBranchName), "*" + headBranchName);

        printList("=== Branches ===", branches);
        printList("=== Staged Files ===", adds);
        printList("=== Removed Files ===", removes);
        printList("=== Modifications Not Staged For Commit ===", modsNotStaged);
        printList("=== Untracked Files ===", untracked);
    }

    /**
     * 1. checkout -- [file name]
     * This should take version of file from head commit and put in working dir (overwrites)
     * 2. checkout [commit id] -- [file name]
     * Does the same as option 1, but from the commit based on the  given id instead.
     * 3. checkout [branchName]
     * Takes all files in front commit of given Branch, puts in cwd
     * (overwriting), staging is cleared, files tracked in curr branch
     * but not given branch are deleted. given branch will be considered head
     *
     * @param args - formatted as {"checkout", options...}
     */
    public static void checkout(String[] args) {
        switch (args.length) {
            case 3 -> checkoutV1(args); // {"checkout", "--", fileName}
            case 4 -> checkoutV2(args); // {"checkout", commitId, "--", fileName}
            case 2 -> checkoutBranch(args); // {"checkout", branchName}
            default -> printAndExit("Incorrect operands.");
        }
    }

    /**
     * Creates new branch with given name, points it at curr head,
     *
     * @param args - formatted as {"branch", branchName}
     */
    public static void branch(String[] args) {
        verifyOperandLen(args, 2);
        String branchName = args[1];
        validate(!branchExists(branchName), "A branch with that name already exists.");

        createNewBranch(branchName, getHeadCommitHash());
    }

    /**
     * Deletes branch with given name
     * (only deletes pointer associated with branch)
     *
     * @param args - formatted as {"rm-branch", branchName}
     */
    public static void removeBranch(String[] args) {
        verifyOperandLen(args, 2);
        String branchName = args[1];
        validate(branchExists(branchName), "A branch with that name does not exist.");
        validate(!branchName.equals(getHeadBranchName()),
                "Cannot remove the current branch.");

        Utils.join(BRANCHES_DIR, branchName).delete();
    }

    /**
     * Checks out all tracked files in given commit
     * Removes tracked files not present in given commit
     * moves current branch head to given commit
     * staging area cleared
     *
     * @param args - formatted as {"reset", commitID}
     */
    public static void reset(String[] args) {
        verifyOperandLen(args, 2);
        String requestedCommitHash = shortHandHashChecker(args[1]);

        checkoutCommitAt(requestedCommitHash);
        // Move branch front pointer to new commit
        getHeadBranch().moveFrontTo(requestedCommitHash);
    }

    /**
     * Merges files from given branch into curr branch
     *
     * @param args - formatted as {"merge", branchName}
     */
    public static void merge(String[] args) {
        verifyOperandLen(args, 2);
        validate(getUntrackedFiles(getHeadCommit()).size() == 0,
                "There is an untracked file in the way; delete it, or add and commit it first.");
        validate(!stagedChangesExist(), "You have uncommitted changes.");

        // validate requested branch can be merged:
        String otherBranchName = args[1];
        String currBranchName = getHeadBranchName();
        validate(branchExists(otherBranchName), "A branch with that name does not exist.");
        validate(!otherBranchName.equals(currBranchName),
                "Cannot merge a branch with itself.");

        // head, given, and split commits + validate them:
        Commit head = getHeadCommit(),
                other = getBranchFromName(otherBranchName).getFrontCommit(),
                split = getSplitCommit(otherBranchName, currBranchName); // takes N time
        validate(!split.equals(other), "Given branch is an ancestor of the current branch.");
        if (split.equals(head)) {
            checkoutCommitAt(other.getCommitHash());
            printAndExit("Current branch fast-forwarded.");
        }


        // map of files for head, given, and split commits:
        HashMap<String, String> headFiles = head.getSnapShots(),
                otherFiles = other.getSnapShots(),
                splitFiles = split.getSnapShots();
        boolean conflictExists = false;

        // iterate through split commit's files:
        for (String fileName : splitFiles.keySet()) {
            if (isSame(fileName, splitFiles, headFiles)) {
                if (!otherFiles.containsKey(fileName)) {
                    // remove & untrack
                    headFiles.remove(fileName);
                    restrictedDelete(join(CWD, fileName));
                } else if (!isSame(fileName, otherFiles, headFiles)) {
                    // checkout from other & stage
                    checkoutFileAt(fileName, other);
                    stageForAdd(fileName);
                }
            } else if (!isSame(fileName, splitFiles, otherFiles)
                    && !isSame(fileName, headFiles, otherFiles)) {
                // merge conflict
                String mergedContent = mergeFiles(fileName, head, other);
                writeContents(join(CWD, fileName), mergedContent);
                conflictExists = true;
            }
        }

        // iterate through given commit's files:
        for (String fileName : otherFiles.keySet()) {
            if (!splitFiles.containsKey(fileName) && !headFiles.containsKey(fileName)) {
                // checkout from other & stage
                checkoutFileAt(fileName, other);
                stageForAdd(fileName);
            }
        }

        // create new commit with content pointing to both branches
        // (should remain on same head branch)
        String mergeMessage = "Merged " + otherBranchName + " into " + currBranchName + ".";
        commit(new String[]{"commit", mergeMessage});
        Commit newlyCreatedCommit = getHeadCommit();
        newlyCreatedCommit.setMergedParentHash(other.getCommitHash());
        newlyCreatedCommit.save();

        if (conflictExists) {
            System.out.println("Encountered a merge conflict.");
        }
        System.exit(0);
    }


    // ==================== Checkout Functions: ====================

    /**
     * @param args - formatted as {"checkout", "--", fileName}
     */
    private static void checkoutV1(String[] args) {
        validate(args[1].equals("--"), "Incorrect operands.");
        checkoutFileAt(args[2], getHeadCommit());
    }

    /**
     * @param args - formatted as {"checkout", commitId, "--", fileName}
     */
    private static void checkoutV2(String[] args) {
        validate(args[2].equals("--"), "Incorrect operands.");
        String requestedCommitHash = shortHandHashChecker(args[1]);

        Commit requestedCommit = getCommitFromHash(requestedCommitHash);
        checkoutFileAt(args[3], requestedCommit);
    }

    /**
     * @param args - formatted as {"checkout", branchName}
     */
    private static void checkoutBranch(String[] args) {
        String branchName = args[1];
        validate(branchExists(branchName), "No such branch exists.");
        validate(!getHeadBranchName().equals(branchName),
                "No need to checkout the current branch.");

        String reqBranchFront = getBranchFromName(branchName).getFrontCommitHash();

        checkoutCommitAt(reqBranchFront);
        saveHeadBranchName(branchName); // move head to curr branch
    }

    // Checkout Helpers:

    /**
     * @param fileName - name of file to checkout
     * @param commit   - commit to checkout from
     */
    private static void checkoutFileAt(String fileName, Commit commit) {
        if (!commit.contains(fileName)) {
            printAndExit("File does not exist in that commit.");
        } else {
            writeContents(join(CWD, fileName), commit.getContentOfFile(fileName));
        }
    }

    /**
     * Should delete all files in cwd that are in curr branch
     * but not in checked-out branch are del
     * Should clear staging area
     * Checkout each file from the requested commit.
     *
     * @param hash - hash of requested commit
     */
    private static void checkoutCommitAt(String hash) {
        validate(getUntrackedFiles(getHeadCommit()).size() == 0,
                "There is an untracked file in the way; delete it, or add and commit it first.");

        Commit requestedCommit = readObject(Utils.join(COMMITS_DIR, hash), Commit.class);
        Commit headCommit = getHeadCommit();
        List<String> cwdFiles = plainFilenamesIn(CWD),
                stagedAdds = plainFilenamesIn(STAGING_ADD_DIR),
                stagedRms = plainFilenamesIn(STAGING_RM_DIR);

        // delete all files in cwd that are in curr branch but
        // not in branch to be checked-out are del:
        for (String elem : cwdFiles) {
            if (headCommit.contains(elem) && !requestedCommit.contains(elem)) {
                restrictedDelete(join(CWD, elem));
            }
        }
        // clear staging:
        for (String elem : stagedAdds) {
            join(STAGING_ADD_DIR, elem).delete();
            restrictedDelete(join(CWD, elem));
        }
        for (String elem : stagedRms) {
            join(STAGING_RM_DIR, elem).delete();
        }
        // Takes all files in front commit of given Branch, puts in cwd (overwriting):
        for (String elem : requestedCommit.getSnapShots().keySet()) {
            checkoutFileAt(elem, requestedCommit);
        }

        // move head to this commit
        saveHeadCommitHash(hash);
    }

    // ==================== Helper Functions: ====================

    /**
     * Verifies if length of args is equal to expectedNumArgs
     */
    private static void verifyOperandLen(String[] args, int expNumArgs) {
        if (args.length != expNumArgs) {
            printAndExit("Incorrect operands.");
        }
    }

    // ==================== Error Handling: ====================

    private static void validate(boolean condition, String errorMessage) {
        if (!condition) {
            printAndExit(errorMessage);
        }
    }

    private static void printAndExit(String message) {
        System.out.println(message);
        System.exit(0);
    }

    /**
     * If the requestedCommitHash is shorter than 40 char,
     * it checks via the shorthand method, otherwise, it
     * normally checks if a commit with that id exists.
     */
    public static String shortHandHashChecker(String requestedCommitHash) {
        int reqHashLen = requestedCommitHash.length();
        if (reqHashLen < 40) {
            for (String elem : plainFilenamesIn(COMMITS_DIR)) {
                if (elem.regionMatches(0, requestedCommitHash, 0, reqHashLen)) {
                    return elem;
                }
            }
        }
        validate(commitExists(requestedCommitHash), "No commit with that id exists.");
        return requestedCommitHash;
    }

}
