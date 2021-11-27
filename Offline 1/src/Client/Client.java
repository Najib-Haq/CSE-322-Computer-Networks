package Client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client{
    static File LOCAL_STORAGE = new File("src/Client/CLIENT_storage");
    static Integer REQ_ID = 0;
    public static PrintStream stdout = System.out;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // delete previous directory and create new one
//        deleteDirectory(LOCAL_STORAGE);
        // make local storage
        Client.LOCAL_STORAGE.mkdir();

        // connect to server
        Socket socket = new Socket("localhost", 6666);
        System.out.println("Establishing Connection...");

        // buffers
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        // connect to server file IO
        Socket socket_file = new Socket("localhost", 6667);
        System.out.println("Establishing File I/O Connection...");
        ObjectOutputStream out_file = new ObjectOutputStream(socket_file.getOutputStream());
        ObjectInputStream in_file = new ObjectInputStream(socket_file.getInputStream());

        // get communication classes
        ClientIO cio = new ClientIO(socket, out, in, socket_file, out_file, in_file);

        // create gui
        GUI gui = new GUI(cio);
        System.out.println(">>Enter your Student ID to login");
    }

    public static void deleteDirectory(File dir){
        if(dir.isDirectory()){
            File[] all_files = dir.listFiles();
            for(int i=0; i<all_files.length; i++) deleteDirectory(all_files[i]);
        }
        dir.delete();
    }
}
