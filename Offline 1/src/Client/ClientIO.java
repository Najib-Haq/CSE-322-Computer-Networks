package Client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientIO implements Runnable{
    Socket socket, socket_file;
    ObjectOutputStream out, out_file;
    ObjectInputStream in, in_file;
    String msg, task; String[] msg_tokenized;
    FileSystemClient fsys;

    public ClientIO(Socket socket, ObjectOutputStream out, ObjectInputStream in,
                    Socket socket_file, ObjectOutputStream out_file, ObjectInputStream in_file){
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.out_file = out_file;
        this.in_file = in_file;
        this.socket_file = socket_file;
        this.fsys = new FileSystemClient(socket_file, out_file, in_file);
    }

    public void setTask(String task){
        this.task = task;
    }

    public boolean login(String ID){
        try{
            this.out.writeObject(ID);
            this.msg = (String) in.readObject();

            if (msg.equalsIgnoreCase("SUCCESS")) {
                System.out.println(">>Successful Login");
                return true;
            }
            else {
                System.out.println(">>Failed Login. Please make sure you have only one login instance.");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("An error occured while connecting");
            return false;
        }
    }

    public void get_response(String command) throws IOException, ClassNotFoundException{
        // ask for student list, active and inactive
        this.out.writeObject(command);
        // get list
        System.out.println(
                ">>Server: " + (String)this.in.readObject()
        );

        if(this.task.equalsIgnoreCase("LOGOUT")) {
            this.socket.close();
            this.socket_file.close();
            System.err.println("Socket closed");
        }
    }

    public void download_file() throws IOException, ClassNotFoundException{
        // this.task should be : DOWNLOAD#FileID#StudentID
        this.out.writeObject(this.task);
        // begin file download
        this.fsys.setMode("download");
        new Thread(this.fsys).start();
    }

    public void upload_file(String privacy, Integer reqID, String filename) throws IOException, ClassNotFoundException{
        // config file system
        this.fsys.setPrivacy(privacy);
        this.fsys.setReqID(reqID);
        this.fsys.setFile(new File(filename));
        this.fsys.setMode("upload");
        // this.task should be : UPLOAD#privacy#filename#filesize#reqID
        this.out.writeObject(this.fsys.getUploadCommand());
        // begin file upload
        new Thread(this.fsys).start();
    }

    @Override
    public void run() {
        try{
            if(this.task.contains("DOWNLOAD#")) download_file();
            else get_response(this.task);
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Unsuccessful requests");
        }
    }

}
