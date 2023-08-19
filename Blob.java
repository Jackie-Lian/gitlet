package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Blob implements Serializable {

    /** Name of the blob.*/
    private String _name;

    /** Content of the blob. */
    private String _content;

    public String getContent() {
        return _content;
    }

    public String getName() {
        return _name;
    }

    /** Returns the SHA1 of the blob. */
    public String getSHA1() {
        List<Object> info = new ArrayList<Object>();
        info.add(_name);
        info.add(_content);
        return Utils.sha1(info);
    }

    public Blob(String filename, String content) {
        _name = filename;
        _content = content;
    }



}
