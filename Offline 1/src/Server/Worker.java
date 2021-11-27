package Server;

import Client.FileSystemClient;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class Worker extends Thread {
    Student student;
    Socket socket, socket_file;
    ObjectOutputStream out, out_file;
    ObjectInputStream in, in_file;
    String msg; String[] msg_tokenized;
    FileSystemServer fsys;

    public Worker(
            Socket socket,
            ObjectOutputStream out,
            ObjectInputStream in,
            Socket socket_file,
            ObjectOutputStream out_file,
            ObjectInputStream in_file
    )
    {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.setFileIO(socket_file, out_file, in_file);
    }

    public boolean setup() throws IOException, ClassNotFoundException{
        // get student ID
        Integer ID = Integer.valueOf((String) this.in.readObject());
        this.student = null;
        for(Student std: Server.connected_students){
            if (std.ID.equals(ID)){
                // if already online
                if(std.status == 1){
                    out.writeObject("Failed. Already Logged In " + ID.toString());
                    System.out.println("Student ID " + std.ID + " already logged in. Disconnected.");
                    return false;
                }
                // re-logged in
                else{
                    this.student = std;
                    this.student.status = 1;
                }
                break;
            }
        }

        out.writeObject("SUCCESS");
        if(this.student == null){
            this.student = new Student(ID, Server.FILE_DIRECTORY, this.socket.getLocalAddress().toString());
            this.student.setMessages(Server.server_copy_msg);
            Server.connected_students.add(student);
        }
        System.out.println("Connected  : " + this.student.ID);

        // initialize
        this.fsys = new FileSystemServer(this.out_file, this.in_file, this.student);
        return true;
    }

    public void setFileIO(Socket socket, ObjectOutputStream out, ObjectInputStream in){
        this.socket_file = socket;
        this.out_file = out;
        this.in_file = in;
    }

    public void sendStudentList() throws IOException{
        String online_students = "Online Students: \n";
        String offline_students = "Offline Students: \n";
        for(Student std: Server.connected_students){
            if(std.status == 1) online_students += "\t" + std.ID + "\n";
            else offline_students += "\t" + std.ID + "\n";
        }
        this.out.writeObject(online_students + offline_students);
    }

    public void sendAllFileList() throws IOException{
        // get list of all students from created folders
        File[] all_files = Server.FILE_DIRECTORY.listFiles();

        String send_msg = "All Filenames: \n";
        if (all_files.length > 0){
            for(File f: all_files){
                send_msg += f.getName() + ":\n";
                File[] public_files = new File(f.getAbsolutePath() + "\\public").listFiles();
                for(File f_public: public_files) send_msg += "\t" + f_public.getName() + "\n";
                send_msg += "\n";
            }
            this.out.writeObject(send_msg);
        }

    }

    public void sendClientFileList() throws IOException{
        // get list of all students from created folders
        String send_msg = "Public Files: \n\tFileID\tFileName\n";
        for(String id: this.student.publicFileIDs){
            send_msg += "\t" + id + "\t" + new File(Server.file_map.get(id)).getName() + "\n";
        }

        send_msg += "Private Files: \n\tFileID\tFileName\n";
        for(String id: this.student.privateFileIDs){
            send_msg += "\t" + id + "\t" + new File(Server.file_map.get(id)).getName() + "\n";
        }

        this.out.writeObject(send_msg);
    }

    public void sendSpecFileList(int ID) throws IOException{
        // get list of specific students from created folders
        File[] all_files;
        for(Student std: Server.connected_students){
            if(std.ID.equals(ID)){
                String send_msg = "All Public Filenames of " + ID + ": \n\tFileID\tFileName\n";
                for(String id: std.publicFileIDs){
                    send_msg += "\t" + id + "\t" + new File(Server.file_map.get(id)).getName() + "\n";
                }
                this.out.writeObject(send_msg);
                return;
            }
        }
        this.out.writeObject("Student ID doesn't exist in server");
    }

    public void sendFile(String fileID) throws IOException, ClassNotFoundException{
        if(Server.file_map.containsKey(fileID)){
            System.out.println("Found FileID: " + fileID + " , FileName: " + Server.file_map.get(fileID));
            // send file to client
            this.fsys.setFilename(Server.file_map.get(fileID));
            this.fsys.setMode("upload"); // server-upload, client-download
            new Thread(this.fsys).start();
            return ;
        }
        System.out.println("FileID: " + fileID + " not found");
        this.out.writeObject("Not#Found");
    }

    public void run()
    {
        File[] files;
        try {
            if(setup()){
                while (this.student.status == 1)
                {

                    this.msg = (String) this.in.readObject();
                    System.out.println(">>From Client : " + this.msg);
                    this.msg_tokenized = this.msg.split("#");

                    switch(this.msg_tokenized[0].toUpperCase()){
                        case "UPLOAD":
                            // test download
                            this.fsys.setMsg(this.msg_tokenized);
                            this.fsys.setReqID(Integer.parseInt(this.msg_tokenized[4]));
                            this.fsys.setMode("download"); // this is server-download; client-upload
                            new Thread(this.fsys).start();
                            break;
                        case "STUDENT_LIST":
                            sendStudentList();
                            break;
                        case "MY_FILE_LIST":
                            sendClientFileList();
                            break;
                        case "OTHER_FILE_LIST":
                            sendSpecFileList(Integer.parseInt(this.msg_tokenized[1]));
                            break;
                        case "DOWNLOAD":
                            sendFile(this.msg_tokenized[1]);
                            break;
                        case "REQUEST":
                            Server.FILE_ID += 1;
                            String send_msg = "File request from Student ID: " + this.student.ID + " : \n\tFile description: " + this.msg_tokenized[1] + " ; \n\tRequest ID : " + Server.FILE_ID;
                            System.out.println(send_msg);
                            for(Student std: Server.connected_students) std.addMessage(send_msg);
                            Server.server_copy_msg.add(send_msg);
                            Server.request_map.put(Server.FILE_ID, this.student);
                            this.out.writeObject("Your requestID is " + Server.FILE_ID);
                            break;
                        case "READ":
                            if(this.student.messagesLeft() == 0) this.out.writeObject("No unread messages");
                            else {
                                this.out.writeObject("All messages:\n" + this.student.readMessages());
                                this.student.clearMessages();
                            }
                            break;
                        case "LOGOUT":
                            this.student.status = 0;
                            this.out.writeObject("LOGGING OUT student ID: " + this.student.ID);
                            System.out.println("LOGGING OUT student ID: " + this.student.ID);
                            this.socket.close();
                            this.socket_file.close();
                            break;
                        default:
                            System.out.println(this.msg_tokenized[0]);
                            System.out.println("Unrecognized Command from Client");
                    }
                }
            }
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("Student ID: " + this.student.ID + " got disconnected.");
            this.student.status = 0;
        }
    }
}
