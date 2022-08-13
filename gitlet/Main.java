package gitlet;

//import jdk.jshell.execution.Util;

import static gitlet.Repository.*;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Brandon Wong
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        if (firstArg.equals("init")) {
            init(args);
        } else if (GITLET_DIR.exists()) {
            switch (firstArg) {
                case "add" -> add(args);
                case "commit" -> commit(args);
                case "rm" -> remove(args);
                case "log" -> log(args);
                case "global-log" -> globalLog(args);
                case "find" -> find(args);
                case "status" -> status(args);
                case "checkout" -> checkout(args);
                case "branch" -> branch(args);
                case "rm-branch" -> removeBranch(args);
                case "reset" -> reset(args);
                case "merge" -> merge(args);
                default -> {
                    System.out.println("No command with that name exists.");
                    System.exit(0);
                }
            }
        } else {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
