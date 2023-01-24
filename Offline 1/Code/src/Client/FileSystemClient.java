package Client;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class FileSystemClient implements Runnable{
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    String msg, privacy, mode, download_path; String[] msg_tokenized;
    Integer reqID;
    File file;

    public FileSystemClient(
            Socket socket,
            ObjectOutputStream out,
            ObjectInputStream in
    ){
        this.socket = socket;
        this.out = out;
        this.in = in;
    }

    public void setPrivacy(String privacy){ this.privacy = privacy; }

    public void setReqID(Integer reqID){ this.reqID = reqID; }

    public void setFile(File file){ this.file = file; }

    public void setMode(String mode){ this.mode = mode; } // upload or download

    public void setDownload_path(String path){ this.download_path = path; }

    public String getUploadCommand(){
        // send privacy filename filesize reqID
        return "UPLOAD#" + this.privacy + "#" + this.file.getName() + "#" + this.file.length() + "#" + this.reqID;
    }

    public void updateProgress(String msg, double chunk, int total, String ack) {
        PrintStream gui_out = System.out;
        System.setOut(Client.stdout);
        final int width = 30; // progress bar width in chars

        System.out.print("\r" + msg + " Chunk [");
        int i = 0;
        for (; i < (int)((chunk/total)*width); i++) {
            System.out.print("=");
        }
        System.out.print(">");
        for (; i < width; i++) {
            System.out.print(" ");
        }
        System.out.print("]" + (int)chunk + "/" + total + " ");
        if(!ack.equals("")) System.out.print("; " + ack + " received ");
//        else System.out.print("\n");
        System.setOut(gui_out);
    }

    // step1
    // returns chunk size. returns -1 if error
    Integer associateFile() throws IOException, ClassNotFoundException{
        // begin file transfer message
        System.out.println(">>Server: " + (String) in.readObject());
        // receive fileID and chunk size
        this.msg = (String) in.readObject();
        this.msg_tokenized = this.msg.split("#");
        System.out.println("Beginning transfer to server now : FileID is " + this.msg_tokenized[0] + " and chunk size is " + this.msg_tokenized[1]);
        return Integer.valueOf(this.msg_tokenized[1]);
    }

    // step 2
    Boolean sendFile(File f, Integer chunksize) throws IOException, ClassNotFoundException, InterruptedException{
        this.socket.setSoTimeout(30000); // set 30sec as time limit
        FileInputStream fis = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(fis);

        byte[] contents;
        long fileLength = f.length();
        long current = 0;
        int total_chunks = (int) Math.ceil((double)fileLength / chunksize);
        String ack_msg = "";

        for(int i=1; i<=total_chunks; i++){
            if(fileLength - current >= chunksize)
                current += chunksize;
            else{
                chunksize = (int)(fileLength - current);
                current = fileLength;
            }
            contents = new byte[chunksize];
//            System.out.println("CHUNK : " + chunksize);
            int success = bis.read(contents, 0, chunksize);
            this.out.writeObject(contents);
            this.out.flush();
            // wait for acknowledgement
            try{
                ack_msg = (String)this.in.readObject();
                updateProgress("Sending", i, total_chunks, ack_msg);
                if(!ack_msg.equals("ACK")) {
                    this.out.flush(); bis.close();
                    return false;
                }
            }catch (SocketTimeoutException e){
                // if timeout
                System.out.println("Server socket timed out. Sending termination command");
                this.out.writeObject("TIME OUT");
                break;
            }
        }
        this.socket.setSoTimeout(0); // return to infinite timeout
        this.out.flush();
        bis.close();
        // send complete message
        this.out.writeObject("COMPLETE");
        ack_msg = (String) this.in.readObject();
        System.out.println(">>Server: " + ack_msg);
        if(ack_msg.equalsIgnoreCase("SUCCESS")){
            System.out.println(f.length() + " Bytes File sent to server successfully!");
            return true;
        }
        else return false;
    }

    public Boolean upload(){
        try{
            int chunk = associateFile();
            return sendFile(this.file, chunk);
        }
        catch(Exception e) {
            return false;
        }
    }

    public Boolean download(){
        try{
            this.msg = (String) this.in.readObject();
            this.msg_tokenized = this.msg.split("#");
            if(this.msg_tokenized[0].equalsIgnoreCase("Not")){
                System.out.println(">>Server: File not found");
                return false;
            }
            String filename = this.msg_tokenized[0];
            int filesize = Integer.parseInt(this.msg_tokenized[1]);
            int chunk_size = Integer.parseInt(this.msg_tokenized[2]);
            // actual download
            FileOutputStream fos = new FileOutputStream(new File(this.download_path + "/" +  filename));
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] contents = new byte[chunk_size];

            int bytesRead = 0;
            int total=0; //how many bytes read
            int total_chunks = (int) Math.ceil((double) filesize / chunk_size);
            for(int i=1; i<=total_chunks; i++)	//loop is continued until received byte=totalfilesize
            {
                contents = (byte[]) this.in.readObject();
                bytesRead = contents.length;
                total += bytesRead;
                bos.write(contents, 0, bytesRead);
//            System.out.println("Received Chunk " + i + "/" + total_chunks);
                this.updateProgress("Received", i, total_chunks, "");
                bos.flush();
            }

            bos.close();
            this.msg = (String) this.in.readObject();
            System.out.println(">>Server: " + this.msg);
            System.out.println("Successfully written " + total + " Bytes in " + this.download_path + "\\" + filename);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {
        System.out.println("Starting Client File IO");

        if(mode.equalsIgnoreCase("upload")) {
            if(!upload()){
                System.out.println("File upload not successful");
            };
        }
        else if(mode.equalsIgnoreCase("download")){
            if(!download()){
                System.out.println("File download not successful");
            };
        }
    }
}
