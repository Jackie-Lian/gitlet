package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Commit implements Serializable {

    /** Stores the MESSAGE of this commit. */
    private String _msg;

    /** Stores the SHA1 of the parent.*/
    private String _parent;

    /** Path to the folder that stores all commits in this repo.*/
    private File _commits = Utils.join(Main.getGitlet(), "commits/");

    /** Stores the SHA1 of the second parent.*/
    private String _secondParent;

    /** Stores the blobs tracked by this commits. KEY is the file name,
     * value is the SHA1 of the file.*/
    private HashMap<String, String> _blobs;

    /** The timestamp of this commit.*/
    private Date _timestamp;

    /** The branch this commit is on.*/
    private String _currBranch;

    public Commit() {
        _parent = "";
        _secondParent = "";
        _timestamp = new Date(0);
        _msg = "initial commit";
        _blobs = new HashMap<>();
        _currBranch = "master";
    }


    public Commit(String msg, String parent, String secondParent,
                  String branch) {
        this._msg = msg;
        this._parent = parent;
        this._secondParent = secondParent;
        _timestamp = new Date();
        Commit parentCommit = Utils.readObject(Utils.join(_commits, parent),
                Commit.class);
        _blobs = parentCommit.getBlobs();
        _currBranch = branch;
    }

    public String getMessage() {
        return _msg;
    }

    public String getParent() {
        return _parent;
    }

    public String getSecondParent() {
        return _secondParent;
    }

    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    public void addBlob(Blob blob) {
        _blobs.put(blob.getName(), blob.getSHA1());
    }

    public void removeBlob(Blob blob) {
        _blobs.remove(blob.getName());
    }

    public Date getTimestamp() {
        return _timestamp;
    }

    public String getBranch() {
        return _currBranch;
    }

    public String getSHA1() {
        List<Object> info = new ArrayList<Object>();
        info.add(_parent);
        info.add(_msg);
        if (_blobs != null) {
            info.addAll(_blobs.values());
        }
        info.add(_timestamp.toString());
        return Utils.sha1(info);
    }




}
