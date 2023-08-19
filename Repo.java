package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Queue;
import java.text.SimpleDateFormat;

import java.util.regex.Matcher;

public class Repo implements Serializable {

    /**
     * KEY is the branch's name, VALUE is the SHA1 of the commit object
     * the branch points to.
     */
    private Map<String, String> _branches;

    /**
     * Stores the remotes, KEY is the remote name and VALUE is the path to
     * the remote directory on this computer.
     */
    private Map<String, String> _remotes;

    /**
     * MASTER points to the latest commit in the master branch.
     */
    private String _master;

    /**
     * HEAD points to the latest commit.
     */
    private String _head;

    /**
     * Stores the name of the branch that is currently active.
     */
    private String _activeBranch;

    /**
     * Stores the files in the staging area. Key is the file name,
     * VALUE is the SHA1 of the current content of the file being staged.
     */
    private Map<String, String> _stagingArea;

    /**
     * Stores the files in the staing area for removal.
     */
    private Map<String, String> _stagedForRemoval;

    /**
     * Path to the commits folder from the current working directory.
     */
    private File _commits = Utils.join(Main.getGitlet(), "commits/");

    /**
     * Path to the blobs folder from the current working directory.
     */
    private File _blobFolder = Utils.join(Main.getGitlet(), "blobs/");

    /**
     * Path to the current working directory.
     */
    private File _cwd = new File(System.getProperty("user.dir"));

    public Repo() throws IOException {
        _branches = new TreeMap<>();
        _remotes = new HashMap<>();
        Commit initial = new Commit();
        _head = _master = initial.getSHA1();
        File initialCommit = Utils.join(_commits, initial.getSHA1());
        initialCommit.createNewFile();
        Utils.writeObject(initialCommit, initial);
        _branches.put("master", _master);
        _activeBranch = "master";
        _stagingArea = new TreeMap<String, String>();
        _stagedForRemoval = new TreeMap<>();
    }

    /**
     * Adds a copy of the file FILENAME as it currently exists to the staging
     * area (see the description of the commit command). For this reason,
     * adding a file is also called staging the file for addition. Staging an
     * already-staged file overwrites the previous entry in the staging area
     * with the new contents. The staging area should be somewhere in .gitlet.
     * If the current working version of the file is identical to the version
     * in the current commit, do not stage it to be added, and remove it from
     * the staging area if it is already there (as can happen when a file is
     * changed, added, and then changed back). The file will no longer be
     * staged for removal (see gitlet rm), if it was at the time of the
     * command.
     */
    public void add(String filename) throws IOException {
        File headFile = Utils.join(_commits, _head);
        File file = Utils.join(_cwd, filename);
        if (!file.exists()) {
            Utils.message("File does not exist.");
            System.exit(0);
        }
        Commit head = Utils.readObject(headFile, Commit.class);
        _stagedForRemoval.remove(filename);
        if (head.getBlobs().containsKey(filename)) {
            String fileSHA = head.getBlobs().get(filename);
            Blob lastBlob = Utils.readObject(Utils.join(_blobFolder, fileSHA),
                    Blob.class);
            if (lastBlob.getContent().
                    equals(Utils.readContentsAsString(file))) {
                if (_stagingArea.containsKey(filename)) {
                    _stagingArea.remove(filename);
                }
            } else {
                Blob newBlob =
                        new Blob(filename, Utils.readContentsAsString(file));
                _stagingArea.put(filename, newBlob.getSHA1());
                File newBlobFile = Utils.join(_blobFolder, newBlob.getSHA1());
                Utils.writeObject(newBlobFile, newBlob);
            }
        } else if (_stagingArea.containsKey(filename)) {
            Blob newBlob = new Blob(filename, Utils.readContentsAsString(file));
            _stagingArea.put(filename, newBlob.getSHA1());
            File newBlobFile = Utils.join(_blobFolder, newBlob.getSHA1());
            Utils.writeObject(newBlobFile, newBlob);
        } else {
            Blob newBlob = new Blob(filename, Utils.readContentsAsString(file));
            File newBlobFile = Utils.join(_blobFolder, newBlob.getSHA1());
            newBlobFile.createNewFile();
            Utils.writeObject(newBlobFile, newBlob);
            _stagingArea.put(filename, newBlob.getSHA1());
        }
    }

    public void commit(String... args) throws IOException {
        String msg = args[0];
        if (_stagingArea.isEmpty() && _stagedForRemoval.isEmpty()) {
            Utils.message("No changes added to the commit.");
            System.exit(0);
        }
        if (msg == null || msg.equals("")) {
            Utils.message("Please enter a commit message.");
            System.exit(0);
        }
        String secondParent = "";
        if (args.length != 1) {
            secondParent = args[1];
        }
        Commit newCommit = new Commit(msg, _head, secondParent, _activeBranch);
        Commit lastCommit =
                Utils.readObject(Utils.join(_commits, _head), Commit.class);
        for (Map.Entry element : _stagingArea.entrySet()) {
            File f = Utils.join(_blobFolder, (String) element.getValue());
            Blob currBlob = Utils.readObject(f, Blob.class);
            if (lastCommit.getBlobs().containsKey(currBlob.getName())) {
                String lastAddress =
                        lastCommit.getBlobs().get(currBlob.getName());
                File lastFile = Utils.join(_blobFolder, lastAddress);
                Blob lastBlob = Utils.readObject(lastFile, Blob.class);
                if (!lastBlob.getContent().equals(currBlob.getContent())) {
                    newCommit.getBlobs().replace(lastBlob.getName(),
                            (String) element.getValue());
                }
            } else {
                newCommit.addBlob(currBlob);
            }
        }
        for (Map.Entry element : _stagedForRemoval.entrySet()) {
            File f = Utils.join(_blobFolder, (String) element.getValue());
            Blob currBlob = Utils.readObject(f, Blob.class);
            newCommit.removeBlob(currBlob);
        }
        _stagingArea.clear();
        _stagedForRemoval.clear();
        _head = newCommit.getSHA1();
        _branches.replace(_activeBranch, _head);
        File newCommitFile = Utils.join(_commits, newCommit.getSHA1());
        newCommitFile.createNewFile();
        Utils.writeObject(newCommitFile, newCommit);
    }

    public void checkout(String filename) throws IOException {
        File prevCommit = Utils.join(_commits, _head);
        Commit prevCommitObj = Utils.readObject(prevCommit, Commit.class);
        if (!prevCommitObj.getBlobs().containsKey(filename)) {
            Utils.message("File does not exist in that commit.");
            return;
        }
        File currVersion = Utils.join(_cwd, filename);
        String commitedFileName = prevCommitObj.getBlobs().get(filename);
        File commitedVersion = Utils.join(_blobFolder, commitedFileName);
        Blob commitedBlob = Utils.readObject(commitedVersion, Blob.class);
        String commitedContent = commitedBlob.getContent();
        if (!currVersion.exists()) {
            currVersion.createNewFile();
        }
        Utils.writeContents(currVersion, commitedContent);
    }

    public void checkout(String commitId, String filename) throws IOException {
        String completeID = abbreviateExists(commitId);
        if (completeID.equals("")) {
            Utils.message("No commit with that id exists.");
            System.exit(0);
        }
        File currVersion = Utils.join(_cwd, filename);
        File prevCommitFile = Utils.join(_commits, completeID);
        Commit prevCommitObj = Utils.readObject(prevCommitFile, Commit.class);
        if (!prevCommitObj.getBlobs().containsKey(filename)) {
            Utils.message("File does not exist in that commit.");
            System.exit(0);
        }
        String commitedFilename = prevCommitObj.getBlobs().get(filename);
        File commitedVersion = Utils.join(_blobFolder, commitedFilename);
        Blob committedBlob = Utils.readObject(commitedVersion, Blob.class);
        String committedContent = committedBlob.getContent();
        if (!currVersion.exists()) {
            currVersion.createNewFile();
        }
        Utils.writeContents(currVersion, committedContent);
    }

    public void checkout(String branchName, boolean boo) throws IOException {
        if (!_branches.containsKey(branchName)) {
            Utils.message("No such branch exists.");
            System.exit(0);
        } else if (_activeBranch.equals(branchName)) {
            Utils.message("No need to checkout the current branch.");
            System.exit(0);
        } else {
            Commit branch = readCommit(_branches.get(branchName));
            Commit head = readCommit(_head);
            for (Map.Entry blobName : branch.getBlobs().entrySet()) {
                File file = Utils.join(_cwd, (String) blobName.getKey());
                if (file.exists()) {
                    if (!head.getBlobs().containsKey(blobName.getKey())) {
                        Utils.message("There is an untracked file in "
                                + "the way; delete it, or add and commit it "
                                + "first.");
                        System.exit(0);
                    }
                    Blob thisBlob = Utils.readObject(Utils.join(_blobFolder,
                            (String) blobName.getValue()), Blob.class);
                    String newContent = thisBlob.getContent();
                    Utils.writeContents(file, newContent);
                } else {
                    file.createNewFile();
                    Blob thisBlob = Utils.readObject(Utils.join(_blobFolder,
                            (String) blobName.getValue()), Blob.class);
                    Utils.writeContents(file, thisBlob.getContent());
                }
            }
            List<String> files = Utils.plainFilenamesIn(_cwd);
            for (int i = 0; i < files.size(); i++) {
                File thisFile = Utils.join(_cwd, files.get(i));
                if (head.getBlobs().containsKey(files.get(i))
                        && !branch.getBlobs().containsKey(files.get(i))) {
                    Utils.restrictedDelete(thisFile);
                }
            }
        }
        _activeBranch = branchName;
        _head = _branches.get(branchName);
        _stagingArea.clear();
    }

    public void log() {
        String currHead = _head;
        while (!currHead.equals("")) {
            Commit currCommit = Utils.readObject(Utils.join(_commits, currHead),
                    Commit.class);
            System.out.println("===");
            System.out.println("commit " + currCommit.getSHA1());
            String pattern = "EEE MMM d HH:mm:ss yyyy Z";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String date = simpleDateFormat.format(currCommit.getTimestamp());
            System.out.println("Date: " + date);
            System.out.println(currCommit.getMessage());
            System.out.println("");
            currHead = currCommit.getParent();
        }
    }

    public void rm(String filename) {
        Commit head = readCommit(_head);
        if (_stagingArea.containsKey(filename)) {
            _stagingArea.remove(filename);
        } else if (head.getBlobs().containsKey(filename)) {
            _stagedForRemoval.put(filename, head.getBlobs().get(filename));
            Utils.restrictedDelete(filename);
        } else {
            Utils.message("No reason to remove the file.");
        }
    }

    public void globalLog() {
        List<String> commits = Utils.plainFilenamesIn(_commits);
        for (String commit : commits) {
            Commit obj = Utils.readObject(Utils.join(_commits, commit),
                    Commit.class);
            System.out.println("===");
            System.out.println("commit " + obj.getSHA1());
            String pattern = "EEE MMM d HH:mm:ss yyyy Z";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String date = simpleDateFormat.format(obj.getTimestamp());
            System.out.println("Date: " + date);
            System.out.println(obj.getMessage());
            System.out.println("");
        }
    }

    public void find(String commitMsg) {
        List<String> commits = Utils.plainFilenamesIn(_commits);
        int count = 0;
        for (String commit : commits) {
            Commit obj = Utils.readObject(Utils.join(_commits, commit),
                    Commit.class);
            if (obj.getMessage().equals(commitMsg)) {
                System.out.println(obj.getSHA1());
                count++;
            }
        }
        if (count == 0) {
            Utils.message("Found no commit with that message.");
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        for (Map.Entry branch : _branches.entrySet()) {
            if (_head.equals(branch.getValue())) {
                System.out.println("*" + branch.getKey());
            } else {
                System.out.println(branch.getKey());
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        for (String filename : _stagingArea.keySet()) {
            System.out.println(filename);
        }
        System.out.println("");

        System.out.println("=== Removed Files ===");
        for (String filename : _stagedForRemoval.keySet()) {
            System.out.println(filename);
        }
        System.out.println("");

        printModifiedNotStaged();
    }

    public void printModifiedNotStaged() {
        Commit currCommit = Utils.readObject(Utils.join(_commits, _head),
                Commit.class);
        HashSet<String> untracked = new HashSet<>();
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> filesInCWD = Utils.plainFilenamesIn(_cwd);
        for (String fileName : filesInCWD) {
            File f = Utils.join(_cwd, fileName);
            if (!currCommit.getBlobs().containsKey(fileName)
                    && !_stagingArea.containsKey(fileName)) {
                untracked.add(fileName);
            } else if (currCommit.getBlobs().containsKey(fileName)) {
                if (!f.exists() && !_stagedForRemoval.containsKey(fileName)) {
                    System.out.println(fileName + " (deleted)");
                }
                String currContent = Utils.readContentsAsString(f);
                Blob blob = Utils.readObject(Utils.join(_blobFolder,
                        currCommit.getBlobs().get(fileName)), Blob.class);
                String committedContent = blob.getContent();
                if (!currContent.equals(committedContent)
                        && !_stagingArea.containsKey(fileName)) {
                    System.out.println(fileName + " (modified) ");
                }
            } else if (_stagingArea.containsKey(fileName)) {
                Blob addedBlob = Utils.readObject(Utils.join(_blobFolder,
                        _stagingArea.get(fileName)), Blob.class);
                String addedContent = addedBlob.getContent();
                if (!f.exists()) {
                    System.out.println(fileName + " (deleted)");
                } else if (!Utils.readContentsAsString(f).
                        equals(addedContent)) {
                    System.out.println(fileName + " (modified)");
                }
            }
        }
        for (Map.Entry file : currCommit.getBlobs().entrySet()) {
            File f = Utils.join(_cwd, (String) file.getKey());
            if (!f.exists() && !_stagedForRemoval.containsKey(file.getKey())) {
                System.out.println(file.getKey() + " (deleted)");
            }
        }
        System.out.println("");
        System.out.println("=== Untracked Files ===");
        for (String fileName : untracked) {
            System.out.println(fileName);
        }
        System.out.println("");
    }

    public void branch(String branchName) {
        if (_branches.containsKey(branchName)) {
            Utils.message("A branch with that name already exists.");
            System.exit(0);
        }
        _branches.put(branchName, _head);
    }

    public void rmBranch(String branchName) {
        if (!_branches.containsKey(branchName)) {
            Utils.message("A branch with that name does not exist.");
            System.exit(0);
        } else if (branchName.equals(_activeBranch)) {
            Utils.message("Cannot remove the current branch.");
            System.exit(0);
        } else {
            _branches.remove(branchName);
        }
    }

    public void reset(String commitID) throws IOException {
        String completeID = abbreviateExists(commitID);
        if (completeID.equals("")) {
            Utils.message("No commit with that id exists.");
            System.exit(0);
        }
        File thisCommit = Utils.join(_commits, completeID);
        Commit commit = Utils.readObject(thisCommit, Commit.class);
        Commit head = Utils.readObject(Utils.join(_commits, _head),
                Commit.class);

        for (Map.Entry blobName : commit.getBlobs().entrySet()) {
            File file = Utils.join(_cwd, (String) blobName.getKey());
            if (file.exists()) {
                if (!head.getBlobs().containsKey(blobName.getKey())) {
                    Utils.message("There is an untracked file in the "
                            + "way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                Blob thisBlob = Utils.readObject(Utils.join(_blobFolder,
                        (String) blobName.getValue()), Blob.class);
                String newContent = thisBlob.getContent();
                Utils.writeContents(file, newContent);
            } else {
                file.createNewFile();
                Blob thisBlob = Utils.readObject(Utils.join(_blobFolder,
                        (String) blobName.getValue()), Blob.class);
                Utils.writeContents(file, thisBlob.getContent());
            }
        }
        List<String> files = Utils.plainFilenamesIn(_cwd);
        for (int i = 0; i < files.size(); i++) {
            File thisFile = Utils.join(_cwd, files.get(i));
            if (head.getBlobs().containsKey(files.get(i))
                    && !commit.getBlobs().containsKey(files.get(i))) {
                Utils.restrictedDelete(thisFile);
            }
        }
        _activeBranch = head.getBranch();
        _stagingArea.clear();
        _branches.replace(_activeBranch, completeID);
        _head = completeID;
    }

    public void merge(String otherBranch) throws IOException {
        boolean hasConflict = false;
        checkMerge(otherBranch);
        String splitPoint = findSplitPoint(otherBranch);
        if (splitPoint.equals(_head)) {
            checkout(otherBranch, true);
            Main.errorAndExit("Current branch fast-forwarded.");
        }
        String otherBranchID = _branches.get(otherBranch);
        if (splitPoint.equals(otherBranchID)) {
            Main.errorAndExit("Given branch is an ancestor of "
                    + "the current branch.");
        }
        Commit other = Utils.readObject(Utils.join(_commits, otherBranchID),
                Commit.class);
        Commit head = Utils.readObject(Utils.join(_commits, _head),
                Commit.class);
        Commit split = Utils.readObject(Utils.join(_commits, splitPoint),
                Commit.class);
        Map<String, String> blobsInSplit = split.getBlobs();
        Map<String, String> blobsInOther = other.getBlobs();
        Map<String, String> blobsInHead = head.getBlobs();
        HashSet<String> allBlobNames =
                joinSets(blobsInSplit, blobsInHead, blobsInOther);
        for (String blobName : allBlobNames) {
            File f = Utils.join(_cwd, blobName);
            if (blobsInSplit.containsKey(blobName)) {
                hasConflict = generalMergeCase1(blobName, otherBranchID,
                        blobsInSplit);
            } else {
                if (!blobsInHead.containsKey(blobName)
                        && blobsInOther.containsKey(blobName)) {
                    if (f.exists()) {
                        Main.errorAndExit("There is an "
                                + "untracked file in the way; delete it, "
                                + "or add and commit it first.");
                    }
                    f.createNewFile();
                    String newContent = readContentFromBranch(blobName,
                            otherBranchID);
                    Utils.writeContents(f, newContent);
                    add(blobName);
                } else if (blobsInHead.containsKey(blobName)
                        && blobsInOther.containsKey(blobName)) {
                    if (!blobsInHead.get(blobName).
                            equals(blobsInOther.get(blobName))) {
                        hasConflict = true;
                        mergeConflictOne(blobName, otherBranchID);
                    }
                }
            }
        }
        String commitMsg = "Merged " + otherBranch + " into "
                + _activeBranch + ".";
        if (hasConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        commit(commitMsg, otherBranchID);
    }

    public void checkMerge(String otherBranch) {
        if (!_stagingArea.isEmpty() || !_stagedForRemoval.isEmpty()) {
            Utils.message("You have uncommitted changes.");
            System.exit(0);
        } else if (!_branches.containsKey(otherBranch)) {
            Utils.message("A branch with that name does not exist.");
            System.exit(0);
        } else if (_activeBranch.equals(otherBranch)) {
            Utils.message("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    public boolean generalMergeCase1(String blobName, String otherBranchID,
                                     Map<String, String> blobsInSplit)
            throws IOException {
        boolean hasConflict = false;
        Commit other = Utils.readObject(Utils.join(_commits, otherBranchID),
                Commit.class);
        Commit head = Utils.readObject(Utils.join(_commits, _head),
                Commit.class);
        Map<String, String> blobsInOther = other.getBlobs();
        Map<String, String> blobsInHead = head.getBlobs();

        String[] result = determineStatus(blobName,
                blobsInSplit.get(blobName), blobsInOther, blobsInHead);
        String modifyStatusHead = result[0];
        String modifyStatusOther = result[1];
        if (modifyStatusHead.equals("same")) {
            if (modifyStatusOther.equals("changed")) {
                checkout(otherBranchID, blobName);
                add(blobName);
            } else if (modifyStatusOther.equals("absent")) {
                rm(blobName);
            }
        } else if (modifyStatusHead.equals("changed")) {
            if (modifyStatusOther.equals("changed")) {
                if (!blobsInHead.get(blobName).
                        equals(blobsInOther.get(blobName))) {
                    mergeConflictOne(blobName, otherBranchID);
                    hasConflict = true;
                }
            } else if (modifyStatusOther.equals("absent")) {
                hasConflict = true;
                mergeConflictTwo(blobName, otherBranchID,
                        "other");
            }
        } else if (modifyStatusHead.equals("absent")) {
            if (modifyStatusOther.equals("changed")) {
                hasConflict = true;
                mergeConflictTwo(blobName, otherBranchID,
                        "head");
            }
        }
        return hasConflict;
    }

    public String[] determineStatus(String blobName, String blobIDAtSplit,
                                    Map<String, String> blobsInOther,
                                    Map<String, String> blobsInHead) {
        String[] result = new String[2];
        if (blobsInOther.containsKey(blobName)) {
            if (blobsInOther.get(blobName).equals(blobIDAtSplit)) {
                result[1] = "same";
            } else {
                result[1] = "changed";
            }
        } else {
            result[1] = "absent";
        }
        if (blobsInHead.containsKey(blobName)) {
            if (blobsInHead.get(blobName).equals(blobIDAtSplit)) {
                result[0] = "same";
            } else {
                result[0] = "changed";
            }
        } else {
            result[0] = "absent";
        }
        return result;
    }

    public HashSet<String> joinSets(Map<String, String> blobs1,
                                    Map<String, String> blobs2,
                                    Map<String, String> blobs3) {
        HashSet<String> result = new HashSet<>();
        for (Map.Entry blob : blobs1.entrySet()) {
            if (!result.contains(blob.getKey())) {
                result.add((String) blob.getKey());
            }
        }
        for (Map.Entry blob : blobs2.entrySet()) {
            if (!result.contains(blob.getKey())) {
                result.add((String) blob.getKey());
            }
        }
        for (Map.Entry blob : blobs3.entrySet()) {
            if (!result.contains(blob.getKey())) {
                result.add((String) blob.getKey());
            }
        }
        return result;
    }

    /**
     * Returns the content of FILENAME from COMMITID, assuming that file with
     * that name exists at that commitID.
     */
    public String readContentFromBranch(String filename, String commitID) {
        Commit branch = Utils.readObject(Utils.join(_commits, commitID),
                Commit.class);
        String blobID = branch.getBlobs().get(filename);
        Blob blob = Utils.readObject(Utils.join(_blobFolder, blobID),
                Blob.class);
        return blob.getContent();
    }

    public void mergeConflictOne(String filename, String otherBranchID)
            throws IOException {
        Commit head = Utils.readObject(Utils.join(_commits, _head),
                Commit.class);
        Commit other = Utils.readObject(Utils.join(_commits, otherBranchID),
                Commit.class);
        File f = Utils.join(_cwd, filename);

        String headVersionID = head.getBlobs().get(filename);
        String otherVersionID = other.getBlobs().get(filename);
        Blob headVersion = Utils.readObject(Utils.join(_blobFolder,
                headVersionID), Blob.class);
        Blob otherVersion = Utils.readObject(Utils.join(_blobFolder,
                otherVersionID), Blob.class);

        String headContent = headVersion.getContent();
        String otherContent = otherVersion.getContent();
        String newContent = "<<<<<<< HEAD\n" + headContent + "=======\n"
                + otherContent + ">>>>>>>\n";

        Utils.writeContents(f, newContent);
        add(filename);
    }

    public void mergeConflictTwo(String filename, String otherBranchID,
                                 String absentOne) throws IOException {
        Commit head = Utils.readObject(Utils.join(_commits, _head),
                Commit.class);
        Commit other = Utils.readObject(Utils.join(_commits, otherBranchID),
                Commit.class);
        File f = Utils.join(_cwd, filename);
        String newContent;

        if (absentOne.equals("head")) {
            f.createNewFile();
            String otherVersionID = other.getBlobs().get(filename);
            Blob otherVersion = Utils.readObject(Utils.join(_blobFolder,
                    otherVersionID), Blob.class);
            String otherContent = otherVersion.getContent();
            newContent = "<<<<<<< HEAD\n" + "=======\n" + otherContent
                    + ">>>>>>>\n";
        } else {
            String headVersionID = head.getBlobs().get(filename);
            Blob headVersion = Utils.readObject(Utils.join(_blobFolder,
                    headVersionID), Blob.class);
            String headContent = headVersion.getContent();
            newContent = "<<<<<<< HEAD\n" + headContent + "=======\n"
                    + ">>>>>>>\n";
        }

        Utils.writeContents(f, newContent);
        add(filename);
    }

    public String findSplitPoint(String otherBranch) {
        String otherID = _branches.get(otherBranch);
        Queue<String> fringe = new ArrayDeque<>();
        fringe.add(otherID);
        HashSet<String> ancestors = new HashSet<>();
        String splitPoint = "";
        while (!fringe.isEmpty()) {
            String commitID = fringe.poll();
            if (!ancestors.contains(commitID)) {
                ancestors.add(commitID);
                Commit thisCommit = Utils.readObject(Utils.join(_commits,
                        commitID), Commit.class);
                if (!thisCommit.getParent().equals("")) {
                    fringe.add(thisCommit.getParent());
                }
                if (!thisCommit.getSecondParent().equals("")) {
                    fringe.add(thisCommit.getSecondParent());
                }
            }
        }

        fringe.add(_head);
        HashSet<String> visited = new HashSet<>();
        while (!fringe.isEmpty()) {
            String commitID = fringe.poll();
            Commit currCommit = Utils.readObject(Utils.join(_commits,
                    commitID), Commit.class);
            if (!visited.contains(commitID)) {
                visited.add(commitID);
                if (ancestors.contains(commitID)) {
                    splitPoint = commitID;
                    break;
                } else {
                    if (!currCommit.getParent().equals("")) {
                        fringe.add(currCommit.getParent());
                    }
                    if (!currCommit.getSecondParent().equals("")) {
                        fringe.add(currCommit.getSecondParent());
                    }
                }
            }
        }
        return splitPoint;
    }

    public void addRemote(String remoteName, String path) {
        if (_remotes.containsKey(remoteName)) {
            Utils.message("A remote with that name already exists.");
            System.exit(0);
        }
        String finalPath = path.replaceAll("/",
                Matcher.quoteReplacement(File.separator));
        _remotes.put(remoteName, finalPath);
    }

    public void rmRemote(String remoteName) {
        if (!_remotes.containsKey(remoteName)) {
            Utils.message("A remote with that name does not exist.");
            System.exit(0);
        }
        _remotes.remove(remoteName);
    }

    public void push(String remoteName, String remoteBranchName)
            throws IOException {
        checkRemoteValid(remoteName, remoteBranchName);
        File remoteDir = new File(_remotes.get(remoteName));
        File commitsFolder = Utils.join(remoteDir, "/commits");
        File repoFile = Utils.join(remoteDir, "/repo");
        Repo repo = Utils.readObject(repoFile, Repo.class);
        Commit currCommit = Utils.readObject(Utils.join(_commits, _head),
                Commit.class);

        if (repo._branches.containsKey(remoteBranchName)) {
            String remoteID = repo._branches.get(remoteBranchName);
            while (!currCommit.getSHA1().equals(remoteID)) {
                File newCommit = Utils.join(commitsFolder, "/"
                        + currCommit.getSHA1());
                newCommit.createNewFile();
                Utils.writeObject(newCommit, currCommit);
                currCommit = Utils.readObject(Utils.join(_commits,
                        currCommit.getParent()), Commit.class);
            }
        } else {
            repo._branches.put(remoteBranchName, _head);
            String commitID = _head;
            while (!commitID.equals("")) {
                currCommit = Utils.readObject(Utils.join(_commits, commitID),
                        Commit.class);
                File newCommit = Utils.join(commitsFolder, "/"
                        + currCommit.getSHA1());
                newCommit.createNewFile();
                Utils.writeObject(newCommit, currCommit);
                commitID = currCommit.getParent();
            }
        }
        repo._branches.replace(remoteBranchName, _head);
        repo._head = _head;
        Utils.writeObject(repoFile, repo);
    }

    public void fetch(String remoteName, String remoteBranchName)
            throws IOException {
        File remoteDir = new File(_remotes.get(remoteName));
        if (!remoteDir.exists()) {
            Main.errorAndExit("Remote directory not found.");
        }
        File repoFile = Utils.join(remoteDir, "/repo");
        File remoteCommits = Utils.join(remoteDir, "/commits");
        File remoteBlobs = Utils.join(remoteDir, "/blobs");
        Repo repo = Utils.readObject(repoFile, Repo.class);
        if (!repo._branches.containsKey(remoteBranchName)) {
            Main.errorAndExit("That remote does not "
                    + "have that branch.");
        }
        String newBranchName = remoteName + "/" + remoteBranchName;
        String commitID = repo._branches.get(remoteBranchName);
        if (!_branches.containsKey(newBranchName)) {
            branch(newBranchName);
        }

        while (!commitID.equals("")) {
            Commit currCommit = Utils.readObject(Utils.join(remoteCommits,
                    "/" + commitID), Commit.class);
            File hereCommit = Utils.join(_commits, currCommit.getSHA1());
            if (!hereCommit.exists()) {
                hereCommit.createNewFile();
                Utils.writeObject(hereCommit, currCommit);
                for (Map.Entry blob: currCommit.getBlobs().entrySet()) {
                    File hereBlob = Utils.join(_blobFolder,
                            (String) blob.getValue());
                    if (!hereBlob.exists()) {
                        hereBlob.createNewFile();
                        Blob remoteBlob = Utils.readObject(Utils.
                                join(remoteBlobs, "/"
                                        + blob.getValue()), Blob.class);
                        Utils.writeObject(hereBlob, remoteBlob);
                    }
                }
            }
            commitID = currCommit.getParent();
        }
        _branches.put(newBranchName, repo._branches.get(remoteBranchName));
    }

    public void pull(String remoteName, String remoteBranchName)
            throws IOException {
        fetch(remoteName, remoteBranchName);
        String newBranchName = remoteName + "/" + remoteBranchName;
        merge(newBranchName);
    }

    public void checkRemoteValid(String remoteName, String remoteBranchName) {
        File remoteDir = new File(_remotes.get(remoteName));
        if (!remoteDir.exists()) {
            Main.errorAndExit("Remote directory not found.");
        } else {
            File repoFile = Utils.join(remoteDir, "/repo");
            HashSet<String> history = findHistory(_head);
            Repo repo = Utils.readObject(repoFile, Repo.class);
            if (repo._branches.containsKey(remoteBranchName)) {
                String commitID = repo._branches.get(remoteBranchName);
                if (!history.contains(commitID)) {
                    Main.errorAndExit("Please pull down remote "
                            + "changes before pushing.");
                }
            }
        }
    }

    public HashSet<String> findHistory(String commitID) {
        Queue<String> fringe = new ArrayDeque<>();
        fringe.add(commitID);
        HashSet<String> ancestors = new HashSet<>();
        while (!fringe.isEmpty()) {
            String iD = fringe.poll();
            if (!ancestors.contains(iD)) {
                ancestors.add(iD);
                Commit thisCommit = Utils.readObject(Utils.join(_commits,
                        iD), Commit.class);
                if (!thisCommit.getParent().equals("")) {
                    fringe.add(thisCommit.getParent());
                }
                if (!thisCommit.getSecondParent().equals("")) {
                    fringe.add(thisCommit.getSecondParent());
                }
            }
        }
        return ancestors;
    }
    public Commit readCommit(String commitID) {
        File address = Utils.join(_commits, commitID);
        Commit obj = Utils.readObject(address, Commit.class);
        return obj;
    }

    public String abbreviateExists(String shortID) {
        String completeID = "";
        List<String> allCommits = Utils.plainFilenamesIn(_commits);
        for (int i = 0; i < allCommits.size(); i++) {
            if (allCommits.get(i).startsWith(shortID)) {
                completeID = allCommits.get(i);
            }
        }
        return completeID;
    }
}
