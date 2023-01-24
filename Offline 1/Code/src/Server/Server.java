package Server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

public class Server {
    static File FILE_DIRECTORY = new File("src/Server/SERVER_storage");
    static ArrayList<Student> connected_students = new ArrayList<>();
    static ArrayList<String> server_copy_msg = new ArrayList<>();
    static Queue<FileSystemServer> upload_queue = new LinkedList<>();
    static Hashtable<Integer, Student> request_map = new Hashtable<Integer, Student>();
    static Hashtable<String, String> file_map = new Hashtable<>();
    public static Integer MAX_BUFFER_SIZE = 500000000, BUFFER_SIZE = 0;
    public static Integer MIN_CHUNK_SIZE = 2048, MAX_CHUNK_SIZE = 8192;
    public static Integer FILE_ID = 0;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // delete previous directory and create new one
        deleteDirectory(FILE_DIRECTORY);
        FILE_DIRECTORY.mkdir();

        ServerSocket welcomeSocket = new ServerSocket(6666);
        ServerSocket welcomeSocket_file = new ServerSocket(6667);

        while(true) {
            System.out.println("Waiting for connection...");
            try{
                Socket socket = welcomeSocket.accept();

                // buffers
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // for multitasking File IO
                System.out.println("Waiting for file I/O connection...");
                Socket socket_file = welcomeSocket_file.accept();
                ObjectOutputStream out_file = new ObjectOutputStream(socket_file.getOutputStream());
                ObjectInputStream in_file = new ObjectInputStream(socket_file.getInputStream());

                // open thread
                Thread worker = new Worker(socket, out, in, socket_file, out_file, in_file);
                worker.start();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    public static void deleteDirectory(File dir){
        if(dir.isDirectory()){
            File[] all_files = dir.listFiles();
            for(int i=0; i<all_files.length; i++) deleteDirectory(all_files[i]);
        }
        dir.delete();
    }
}

