# Gitlet Design Document
author: Jackie Lian

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design
document should be written in markdown, a language that allows you to nicely 
format and style a text file. Organize your design document in a way that 
will make it easy for you or a course-staff member to read.  

## 1. Classes and Data Structures

Commit

Field
1. private transient Commit _parent
2. private transient Commit _secondParent
3. String _hashParent = Util.sha1(_parent): sha1 for parent
4. String _hashSecondParent = Util.sha1(_secondParent): sha1 for second parent
5. Date time
6. String message
7. Map

1. String _parent: stores the sha1 value of the parent
2. String _secondParent: stores the sha1 value of the second parent
3. String message
4. HashMap <String, Blob>: key is the file’s name and value is the Blob object
5. Data _time: commit time
6. String _commitID: content+time+blobs gets hashed into its commitID

Method
1. Commit getParent(): returns the parent of the current Commit object
2. Commit getSecondParent(): returns the second parent of the current Commit object
3. HashMap<String, Blob> getBlobs(): returns the blobs of this commit object
4. String getMessage(): returns the message of this Commit object


Blob

Field
1. String _content

Repo

Field
  1. Final String HEAD: stores the sha1_id of the head
  2. StagingArea _area
  3. File _

Method
  4. init(): initialize the repo if a gitlet version-control system doesn’t exist in the current directory
  5. String getHead(): return the HEAD of the repo ?
  6. StagingArea getStage(): return the staging area of the repo
  7. void add(String file): add a file to the staging area
  8. commit(String msg): takes a snapshot of the current directory and make a commit
  9. void rm(String filename): remove the given file
  10. void log(): prints out the log history of the repo
  11. checkout (String id)
  12. reset(String id): reset the repo back to its version in id
  13. merge

### Staging Area

Field
  1. HashMap<String, String> _addedFiles
  2. ArrayList<String> _removedFiles

Method
  3. add(String fileName): add the file to the staging area
  4. clear(): clear the current staging area (possibly after one commit)
  5. getAddedFiles(): returns the files added to the staging area
  6. getRemovedFiles(): returns the files being removed



## 2. Algorithms

This is where you tell us how your code works. For each class, include
a high-level description of the methods in that class. That is, do not
include a line-by-line breakdown of your code, but something you would
write in a javadoc comment above a method, ***including any edge cases
you are accounting for***. We have read the project spec too, so make
sure you do not repeat or rephrase what is stated there.  This should
be a description of how your code accomplishes what is stated in the
spec.


The length of this section depends on the complexity of the task and
the complexity of your design. However, simple explanations are
preferred. Here are some formatting tips:

* For complex tasks, like determining merge conflicts, we recommend
  that you split the task into parts. Describe your algorithm for each
  part in a separate section. Start with the simplest component and
  build up your design, one piece at a time. For example, your
  algorithms section for Merge Conflicts could have sections for:

   * Checking if a merge is necessary.
   * Determining which files (if any) have a conflict.
   * Representing the conflict in the file.
  
* Try to clearly mark titles or names of classes with white space or
  some other symbols.

## 3. Persistence

Describe your strategy for ensuring that you don’t lose the state of your program
To persist the current status of the repo, we will need to save the current state of commit tree after we make each commit. To do this:

1. write the commit tree to strings and serialize them into bytes that we can write to a specially named files in the file system.
2. write all the commits to the file. We can serialize the objects and write them to files. This could be done with the Utils.writeObject method from the Utils class. We will make sure that our Repo Class and Commit Class implements the Serializable interface.

To retrieve our state, before executing any code, we need to search for the saved files in the working directory and load the commits that we saved in them.

add [filename]

- When a file is added to the staging area, we put it into the HashMap of files which stores all the files being added since the last commit

checkout [commitID]

- check for all the files in the snapshot whether it has been modified or not. if it has been modified, revert it back to its state at the commitID.

## 4. Design Diagram

Attach a picture of your design diagram illustrating the structure of your
classes and data structures. The design diagram should make it easy to 
visualize the structure and workflow of your program.
!![Design Diagram](/Users/lianjiaqi/Desktop/Berkeley/Design Diagram.jpg)
