package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Jackie Lian
 */
public class Main implements Serializable {

    /** Stores the repo object.*/
    private static Repo _repo;

    /** Path to the current working directory.*/
    private static File _cwd = new File(System.getProperty("user.dir"));

    /** Path to the hidden .gitlet folder within the current working
     * directory.*/
    private static File _gitlet = Utils.join(_cwd, ".gitlet/");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            errorAndExit("Please enter a command.");
        }
        if (args[0].equals("init")) {
            init();
            return;
        }
        File repoFile = Utils.join(_gitlet, "repo");
        if (!repoFile.exists()) {
            errorAndExit("Not in an initialized Gitlet directory.");
        }
        _repo = Utils.readObject(repoFile, Repo.class);
        if (args[0].equals("add")) {
            _repo.add(args[1]);
            Utils.writeObject(repoFile, _repo);
        } else if (args[0].equals("commit")) {
            _repo.commit(args[1]);
            Utils.writeObject(repoFile, _repo);
        } else if (args[0].equals("checkout")) {
            if (args.length == 3) {
                if (!args[1].equals("--")) {
                    errorAndExit("Incorrect operands.");
                }
                _repo.checkout(args[2]);
            } else if (args.length == 4) {
                if (!args[2].equals("--")) {
                    errorAndExit("Incorrect operands.");
                }
                _repo.checkout(args[1], args[3]);
            } else if (args.length == 2) {
                _repo.checkout(args[1], true);
            } else {
                errorAndExit("Incorrect operands.");
            }
            Utils.writeObject(repoFile, _repo);
        } else if (args[0].equals("log")) {
            _repo.log();
            Utils.writeObject(repoFile, _repo);
        } else if (args[0].equals("global-log")) {
            _repo.globalLog();
            Utils.writeObject(repoFile, _repo);
        } else if (args[0].equals("rm")) {
            _repo.rm(args[1]);
            Utils.writeObject(repoFile, _repo);
        } else if (args[0].equals("find")) {
            _repo.find(args[1]);
        } else if (args[0].equals("status")) {
            _repo.status();
        } else if (args[0].equals("branch")) {
            _repo.branch(args[1]);
        } else if (args[0].equals("rm-branch")) {
            _repo.rmBranch(args[1]);
        } else if (args[0].equals("reset")) {
            _repo.reset(args[1]);
        } else {
            main2(args);
        }
        Utils.writeObject(repoFile, _repo);
    }

    public static void main2(String... args) throws IOException {
        File repoFile = Utils.join(_gitlet, "repo");
        _repo = Utils.readObject(repoFile, Repo.class);
        if (args[0].equals("merge")) {
            _repo.merge(args[1]);
        } else if (args[0].equals("add-remote")) {
            _repo.addRemote(args[1], args[2]);
        } else if (args[0].equals("rm-remote")) {
            _repo.rmRemote(args[1]);
        } else if (args[0].equals("push")) {
            _repo.push(args[1], args[2]);
        } else if (args[0].equals("fetch")) {
            _repo.fetch(args[1], args[2]);
        } else if (args[0].equals("pull")) {
            _repo.pull(args[1], args[2]);
        } else {
            errorAndExit("No command with that name exists.");
        }
        Utils.writeObject(repoFile, _repo);
    }
    public static void init() throws IOException {
        File cwd = new File(System.getProperty("user.dir"));
        File git = Utils.join(cwd, ".gitlet/");
        if (git.exists()) {
            errorAndExit("A Gitlet version-control system "
                    + "already exists in the current directory.");
        } else {
            git.mkdir();
            File commits = Utils.join(git, "commits/");
            commits.mkdir();
            File blobs = Utils.join(git, "blobs/");
            blobs.mkdir();
            File repo = Utils.join(git, "repo");
            repo.createNewFile();
            _repo = new Repo();
            Utils.writeObject(repo, _repo);
        }

    }

    public static File getGitlet() {
        return _gitlet;
    }

    public static void errorAndExit(String errorMessage) {
        Utils.message(errorMessage);
        System.exit(0);
    }

}
