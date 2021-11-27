package Server;

import java.io.*;
import java.util.Random;

public class FileSystemServer implements Runnable{
    ObjectOutputStream out;
    ObjectInputStream in;
    BufferedOutputStream bos; // need this as member variable for deleting files in special cases
    String msg, filename, fileID, privacy, mode; String[] msg_tokenized;
    Integer filesize = 0, reqID;
    Student std;

    public FileSystemServer(ObjectOutputStream out, ObjectInputStream in, Student std){
        this.out = out;
        this.in = in;
        this.std = std;
    }

    public void setFilename(String filename){ this.filename = filename; }

    public void setMsg(String[] msg_tokenized){ this.msg_tokenized = msg_tokenized; }

    public void setMode(String mode){ this.mode = mode; } // mode = upload / download

    public void setReqID(Integer reqID){ this.reqID = reqID; }


    public void updateProgress(String msg, double chunk, int total) {
        final int width = 50; // progress bar width in chars

        System.out.print("\r" + msg + " Chunk [");
        int i = 0;
        for (; i < (int)((chunk/total)*width); i++) {
            System.out.print("=");
        }
        System.out.print(">");
        for (; i < width; i++) {
            System.out.print(" ");
        }
        System.out.print("]" + (int)chunk + "/" + total + "  ");
    }

    // step1
    // returns chunk size. returns -1 if error
    Integer associateFile() throws IOException, ClassNotFoundException{
        // receive filename and filesize
        // e.g. UPLOAD#PRIVATE/PUBLIC#a.txt#1200
        File path;
        if(this.msg_tokenized[1].equalsIgnoreCase("PRIVATE")) {
            path = this.std.private_location;
            this.privacy = "private";
        }
        else{
            path = this.std.public_location;
            this.privacy = "public";
        }
        this.filename = new String(path.getAbsolutePath() + "\\" + this.msg_tokenized[2]);
        this.filesize = Integer.valueOf(this.msg_tokenized[3]);
        System.out.println("Received " + this.msg_tokenized[2] + " and size : " + filesize + " bytes");


        // check buffer constraints
        if(this.filesize + Server.BUFFER_SIZE > Server.MAX_BUFFER_SIZE) {
            synchronized (this){
                try{
                    System.out.println("Buffer size not enough. Sending Client " + this.std.ID + " to Queue ..");
                    this.out.writeObject("In Queue. File transfer will begin when buffer has space");
                    Server.upload_queue.add(this);
                    this.wait();
                    System.out.println("Restarting file upload for Client " + this.std.ID);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        else {
            Server.BUFFER_SIZE += this.filesize; // if no queue, then allocate buffer now
            this.out.writeObject("Beginning File Transfer");
        }

        int chunk_size = new Random().nextInt(
                (Server.MAX_CHUNK_SIZE - Server.MIN_CHUNK_SIZE) + 1
        ) + Server.MIN_CHUNK_SIZE;

        // send fileID and chunk size
        if(this.msg_tokenized[4].equalsIgnoreCase("-1")){
            Server.FILE_ID += 1;
            this.fileID = String.valueOf(Server.FILE_ID);
        }
        else this.fileID = this.msg_tokenized[4];
        this.out.writeObject(this.fileID + "#" + chunk_size);
        return chunk_size;
    }

    // step2
    Boolean receiveFile(Integer chunksize) throws IOException, ClassNotFoundException, InterruptedException{
        boolean simulate_timeout = false; // simulate timeout
        FileOutputStream fos = new FileOutputStream(this.filename);
        this.bos = new BufferedOutputStream(fos);
        byte[] contents = new byte[chunksize];
        Object ob;

        int bytesRead = 0;
        int total=0; //how many bytes read
        int total_chunks = (int) Math.ceil((double)this.filesize / chunksize);

        for(int i=1; i<=total_chunks; i++)	//loop is continued until received byte=totalfilesize
        {
            ob = this.in.readObject();
            // if timeout occurs
            if(ob.getClass() == String.class){
                System.out.println("Client " + this.std.ID + " : " + (String)ob);
                bos.flush(); bos.close();
                return false;
            }
            contents = (byte[]) ob;
            bytesRead = contents.length;
            this.updateProgress("Received", i, total_chunks);
//            System.out.println("CHUNK : " + bytesRead);
            total += bytesRead;
            bos.write(contents, 0, bytesRead);
            bos.flush();
            // send acknowledgment or
            // simulate timeout condition
            if(!(simulate_timeout && i==10)) this.out.writeObject("ACK");
        }
        bos.close();

        String complete_msg = (String) this.in.readObject();
        System.out.println(">>Client : " + complete_msg);
        if(!complete_msg.equalsIgnoreCase("COMPLETE")){
            System.out.println("Client didn't send completion message. Discarding");
            return false;
        }
        if(total == filesize)
        {
            System.out.println("\nSuccessfully written " + total + " Bytes in " + this.filename);
            return true;
        }
        else return false;
    }

    public Boolean download(){
        Boolean success = false;
        try{
            int chunk = associateFile();
            success = receiveFile(chunk);
            if(!success) {
                this.out.writeObject("FAILURE");
            }
            else{
                this.out.writeObject("SUCCESS");
                Server.file_map.put(this.fileID, this.filename);
                // if this is according to a request, the file will be found in public

                if(this.privacy.equals("private") && (this.reqID == -1)) this.std.addPrivateFID(this.fileID);
                else this.std.addPublicFID(this.fileID);

                // handle request upload
                if(this.reqID != -1){
                    if(Server.request_map.containsKey(this.reqID)) {
                        Server.request_map.get(this.reqID).addMessage(
                                "Student ID: " + this.std.ID + " has fulfilled your file request. Request ID: " + this.reqID
                        );
                    } else System.out.println("No such request ID exist");
                }
            }


            Server.BUFFER_SIZE -= this.filesize; // this is done at the end of try(for queue), also in the catch clause (for unsuccessful transfer cases)
            // notify a thread if filesize of front of queue satisfies constraint
            int sync_filesize = 0;
            while(Server.upload_queue.size() > 0){
                sync_filesize = Server.upload_queue.peek().filesize;
                if(sync_filesize + Server.BUFFER_SIZE < Server.MAX_BUFFER_SIZE){
                    synchronized(Server.upload_queue.peek()){
                        Server.upload_queue.poll().notify();
                        Server.BUFFER_SIZE += sync_filesize; // fill up buffer again
                    }
                }
            }
        }catch(IOException | ClassNotFoundException | InterruptedException e){
            Server.BUFFER_SIZE -= this.filesize;
            try{
                this.bos.close();
            }catch (Exception exp) {
                System.out.println("Cannot close file output stream");
            }

            return false;
        }
        return success;
    }

    public Boolean send(){
        try{
            File f = new File(this.filename);
            this.out.writeObject(f.getName() + "#" + f.length() + "#" + Server.MAX_CHUNK_SIZE);

            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);

            byte[] contents;
            long fileLength = f.length();
            long current = 0;
            int total_chunks = (int) Math.ceil((double)fileLength / Server.MAX_CHUNK_SIZE);
            int chunck_size = Server.MAX_CHUNK_SIZE;

            for(int i=1; i<=total_chunks; i++){
                if(fileLength - current >= chunck_size)
                    current += chunck_size;
                else{
                    chunck_size = (int)(fileLength - current);
                    current = fileLength;
                }
                contents = new byte[chunck_size];
                int success = bis.read(contents, 0, chunck_size);
                this.out.writeObject(contents);
                this.updateProgress("Sending", i, total_chunks);
//            System.out.println("Sending Chunk " + i + "/" + total_chunks);
            }

            this.out.flush();
            bis.close();
            this.out.writeObject(fileLength + " Bytes File sent successfully!");
            System.out.println("\n" + fileLength+ " Bytes File sent to client successfully!");
            return true;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Something went wrong while uploading to client");
            return false;
        }
    }

    @Override
    public void run() {
        System.out.println("Starting Server File IO");

        if(mode.equalsIgnoreCase("download")){
            if(!download()){
                System.out.println("File download from client not successful");
                boolean res = new File(this.filename).delete(); // delete if not successful
            };
        }
        else if(mode.equalsIgnoreCase("upload")){
            if(!send()){
                System.out.println("File upload to client not successful");
            };
        }

    }
}
