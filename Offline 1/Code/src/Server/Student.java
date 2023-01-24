package Server;

import java.io.File;
import java.util.ArrayList;

public class Student {
    Integer ID, status; // status=0 means offline, 1 means online
    String IP_Address;
    File file_location, private_location, public_location;
    ArrayList<String> messages;
    ArrayList<String> publicFileIDs, privateFileIDs;

    public Student(Integer ID, File file_root, String IP_Address){
        this.ID = ID;
        this.IP_Address = IP_Address;
        this.status = 1;

        // make corresponding folders
        this.file_location = new File(file_root.getAbsolutePath() + '/' + ID.toString());
        this.private_location = new File(this.file_location.getAbsolutePath() + "/private");
        this.public_location = new File(this.file_location.getAbsolutePath() + "/public");
        if(!this.file_location.mkdir() &
            !this.private_location.mkdir() &
            !this.public_location.mkdir()) System.out.println("Something wrong with file system");
        this.privateFileIDs = new ArrayList<>();
        this.publicFileIDs = new ArrayList<>();

//        this.messages = new ArrayList<>();
    }

    // functions related to messages
    public void addMessage(String msg){
        this.messages.add(msg);
    }

    public void setMessages(ArrayList messages) { this.messages = new ArrayList<>(messages);}

    public String readMessages(){
        String unread_msg = "";
        for(int i=1; i<=this.messages.size(); i++){
            unread_msg += i + " : " + this.messages.get(i-1) + "\n";
        }
        return unread_msg;
    }

    public void clearMessages(){
        this.messages.clear();
    }

    public int messagesLeft() { return this.messages.size(); }

    public void addPublicFID(String fileID){ this.publicFileIDs.add(fileID); }

    public void addPrivateFID(String fileID){ this.privateFileIDs.add(fileID); }

    public ArrayList<String> getFileIDs(boolean publicFiles, boolean privateFiles){
        ArrayList<String> temp = new ArrayList<>();

        if(publicFiles) temp.addAll(this.publicFileIDs);
        if(privateFiles) temp.addAll(this.privateFileIDs);
        return temp;
    }
}
