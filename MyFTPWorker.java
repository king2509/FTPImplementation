import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Class For a FTP server
 * One thread for one socket connection
 * @author Zejin Huang
 */
class MyFTPWorker extends Thread{
    // default username & password
    private String USER = "king";
    private String PASS = "0925";

    // bufferSize: set for stream buffer
    private final int bufferSize = 4096;

    /* Control Socket Settings
     * writer: used for transfer control commands
     * quitCMDLoop: flag to quit commands loop
     */
    private Socket controlSocket;
    private BufferedWriter writer = null;
    private BufferedReader reader = null;
    private boolean quitCMDLoop = false;

    /**
     * userStatus
     * 1. Entered username, but not entered password yet
     * 2. Logged in
     */
    private enum userStatus {ENTEREDUSER, LOGGEDIN};
    private userStatus currentStatus;

    // file dir
    private String rootDir;
    private String currentDir;

    // FTP dataSocket
    private Socket dataSocket;

    // required by rename method(rnfm, rnto)
    private String oldFilename;

    // File transferring protocol
    private enum TYPE {ASCII, BIN};
    private TYPE type = null;

    // Worker initiation
    MyFTPWorker(Socket socket){
        this.controlSocket = socket;
        rootDir = System.getProperty("user.dir");
        currentDir = rootDir;
    }

    // Thread main function, run cmd loop
    public void run() {
        System.out.println("Thread starts to run!");
        try {
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));

            System.out.println("FTP server opens!");
            sendMSGToClient("220 FTP server opens!");

            if(quitCMDLoop) {
                System.out.println("TRUE");
            } else {
                System.out.println("FALSE");
            }
            while(!quitCMDLoop) {
                executeCMD(reader.readLine());
            }
        } catch (IOException e){
            System.out.println("Some exceptions occurred.");
            e.getStackTrace();
        } finally {
            try {
                writer.close();
                reader.close();
                controlSocket.close();
                debugOutput("Socket closed and worked stopped");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *
     * @param request containing commands & extra information
     */
    private void executeCMD(String request) {
        String[] requestInfo = request.split(" ");
        String cmd = requestInfo[0].toUpperCase();
        String args = "";
        if(requestInfo.length > 1)
            args = requestInfo[1];
        try {
            switch (cmd) {
                case "USER":
                    handleUSER(args);
                    break;

                case "PASS":
                    handlePASS(args);
                    break;

                case "PWD":
                    handlePWD();
                    break;

                case "MKD":
                    handleMKD(args);
                    break;

                case "DELE":
                    handleDELE(args);
                    break;

                case "CWD":
                    handleCWD(args);
                    break;

                case "RMD":
                    handleRMD(args);
                    break;

                case "CDUP":
                    handleCDUP();
                    break;

                case "RETR":
                    handleRETR(args);
                    break;

                case "QUIT": {
                    handleQUIT();
                    break;
                }

                case "TYPE":
                    handleTYPE(args);
                    break;

                case "SIZE":
                    handleSize(args);
                    break;

                case "PASV":
                    handlePASV();
                    break;
                case "RNFR":
                    handleRNFR(args);
                    break;
                case "RNTO":
                    handleRNTO(args);
                    break;

                case "LIST":
                    handleLIST(args);
                    break;

                case "NLST":
                    handleNLST(args);
                    break;

                case "STOR":
                    handleSTOR(args);
                    break;

                default:{
                    sendMSGToClient("550 wait for fulfilling");
                }
            }
        } catch (IOException e) {
            e.getStackTrace();
        }
    }

    private void handleUSER(String username) throws IOException{
        if(currentStatus == userStatus.LOGGEDIN) {
            sendMSGToClient("530 user has logged in");
        } else if(username.equals(USER)) {
            currentStatus = userStatus.ENTEREDUSER;
            sendMSGToClient("331 PASS required");
        } else {
            sendMSGToClient("530 wrong username");
        }
    }

    private void handlePASS(String pass) throws IOException{
        if(currentStatus == userStatus.LOGGEDIN) {
            sendMSGToClient("530 user has logged in");
        } else if(currentStatus == userStatus.ENTEREDUSER) {
            if(pass.equals(PASS)) {
                currentStatus = userStatus.LOGGEDIN;
                sendMSGToClient("230 Logged in successfully");
            }
            else
                sendMSGToClient("530 Wrong password");
        } else {
            sendMSGToClient("230 Logged in successfully");
        }
    }

    // Return current working directory
    private void handlePWD() throws IOException {
        // Check whether user logs in
        if(isLoggedIn())
            sendMSGToClient(String.format("%d \"%s\" is current directory",257,currentDir));
    }

    /**
     *
     * @param dir change directory to "dir", make sure dir specified exists first.
     * @throws IOException Exception may occur with sendMSGToClient
     */
    private void handleCWD(String dir) throws IOException {
        if(isLoggedIn()) {
            // Check whether dir exists
            ArrayList<String> filenames = getPWDFilenames();
            if (!filenames.contains(dir)) {
                sendMSGToClient("550 No such directory");
                return;
            } else {
                currentDir = currentDir + "/" + dir;
            }

            sendMSGToClient("250 current directory: " + currentDir);
        }
    }

    /**
     *
     * @param newDir A new directory made in server
     * @throws IOException Exception with sendMSGTOClient
     */
    private void handleMKD(String newDir) throws IOException {
        if(isLoggedIn()) {
            File file = new File(newDir);
            // Check if file exists
            if(!file.exists()) {
                boolean isMKDSuccess = file.mkdir();
                if(isMKDSuccess)
                    sendMSGToClient(String.format("257 \"%s\" created.",newDir));
                else
                    sendMSGToClient(String.format("450 Failed to create \"%S\"", newDir));
            } else {
                sendMSGToClient("550 Cannot create a file when that file already exists.");
            }
        }
    }

    // Change into upper directory
    private void handleCDUP() throws IOException{
        //No upper directory as root directory
        if(currentDir.equals(rootDir)) {
            sendMSGToClient("250 Already in root directory.");
            return;
        }

        File file = new File(currentDir);
        currentDir = file.getParent();
        sendMSGToClient("250 Current directory is " + currentDir);
    }

    /**
     * Delete file specified, not including directories
     * @param filename file to delete
     * @throws IOException sendMSGToClient exception
     */
    private void handleDELE(String filename) throws IOException{
        // Check whether file exists
        ArrayList<String> filenames = getPWDFilenames();
        if(!filenames.contains(filename))
            sendMSGToClient("550 No such file");
        else {
            File file = new File(filename);
            // In case thate filename is a directory not a file.
            if(!file.isFile()) {
                sendMSGToClient("550 " + filename + " is a directory, not a file.");
                return;
            }
            boolean isDeleted = file.delete();
            if(isDeleted)
                sendMSGToClient(String.format("%d Delete \"%s\" successfully", 250, filename));
            else {
                sendMSGToClient("550 Failed to delete " + filename);
            }
        }
    }

    /**
     *
     * @param dirName directory to be deleted
     * @throws IOException
     */
    private void handleRMD(String dirName) throws IOException{
        ArrayList<String> filenames = getPWDFilenames();
        if(!filenames.contains(dirName)) {
            sendMSGToClient("550 No such file or directory.");
        } else {
            File file = new File(dirName);
            // In case that dirName specified is a file, not a file.
            if(!file.isDirectory()) {
                sendMSGToClient("550 " + dirName +" is a directory, not a file.");
                return;
            }
            boolean isDelete = file.delete();
            if(isDelete)
                sendMSGToClient("250 Removed target directory successfully.");
            else
                sendMSGToClient("550 Failed to remove target directory.");
        }
    }

    // Quit command loop by setting quitCMDLoop as "true"
    private void handleQUIT() throws IOException{
        if(isLoggedIn()) {
            sendMSGToClient("221 The connection closed");
            quitCMDLoop = true;
        }
    }

    /**
     *
     * @param filename target file requesting its size
     * @throws IOException sendMSGToClient
     */
    private void handleSize(String filename) throws IOException{
        if(isLoggedIn()) {
            File file = new File(filename);
            if(!file.exists()) {
                sendMSGToClient("450 No such file.");
            } else if(file.isDirectory()) {
                sendMSGToClient("450 " + filename + "is a directory which has no size property.");
            } else {
                long size = file.length();
                sendMSGToClient("213 "+size);
            }
        }
    }

    // Entering PASV Mode
    private void handlePASV() throws IOException{
        if(isLoggedIn()) {
            // serverSocket allocate port automatically with initial port as 0
            // Port will be -1 when without any initial value
            ServerSocket serverSocket = new ServerSocket(0);
            // Divide port number to port1 & port2
            int port1 = serverSocket.getLocalPort() / 256;
            int port2 = serverSocket.getLocalPort() % 256;
            System.out.println("PASV port: " + serverSocket.getLocalPort());
            sendMSGToClient(String.format("227 Entering Passive Mode (192,168,73,1,%d,%d)", port1, port2));
            dataSocket = serverSocket.accept();
        }
    }

    /**
     *
     * @param filename File requested by client
     * @throws IOException
     */
    private void handleSTOR(String filename) throws IOException{
        if(isLoggedIn()) {
            if(dataSocket == null)
                sendMSGToClient("450 PASV command needed first.");
            else {
                sendMSGToClient("125 Data connection already open; Transfer starting.");
                File file = new File(filename);
                // check if file exists
                if(!file.exists()) {
                    sendMSGToClient("550 No such file");
                    return;
                }
                // check if file is a directory
                if(file.isDirectory()) {
                    sendMSGToClient("550 " + filename + "is a directory.");
                    return;
                }
                // establish data transferring link
                BufferedInputStream in = new BufferedInputStream(dataSocket.getInputStream());
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[bufferSize];
                while (in.read(buffer) != -1) {
                    out.write(buffer);
                }
                out.flush();
                out.close();
                in.close();
                dataSocket.close();
                sendMSGToClient("226 Transfer complete.");
            }
        }
    }

    private void handleRNFR(String oldFilename) throws IOException {
        File file = new File(String.join("/",currentDir, oldFilename));
        if(!file.exists())
            sendMSGToClient("550 No such file or directory.");
        else {
            sendMSGToClient("350 Requested file action pending further information");
            this.oldFilename = oldFilename;
        }
    }

    // Waiting to deal with file existing problem
    private void handleRNTO(String newFilename) throws IOException{
        File file = new File(String.join("/",currentDir,oldFilename));
        File newFile = new File(newFilename);
        if(newFile.exists()) {
            sendMSGToClient("550 target filename exists.");
            return;
        }
        boolean flag = file.renameTo(new File(newFilename));
        if(flag)
            sendMSGToClient("250 RNTO COMMAND successful.");
        else
            sendMSGToClient("550 Failed to accomplish RNTO command");
    }

    /**
     *
     * @param dir List details of all files and directories under dir
     * @throws IOException
     */
    private void handleLIST(String dir) throws IOException {
        if(dataSocket == null) {
            sendMSGToClient("550 PASV Mode needed first.");
            return;
        }

        File[] files = new File(dir).listFiles();
        if(files != null) {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(dataSocket.getOutputStream(), StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter);
            sendMSGToClient("125 Data connection already open; Transfer starting.");
            String pattern = "yyyy-MM-dd\tHH:mm:ss";
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            for (File file : files) {
                String time = format.format(new Date(file.lastModified()));
                String typeOrSize = "";
                typeOrSize += (file.isFile() ? file.length() : "<DIR>");
                String filename = file.getName();
                String line = String.join("\t", time, typeOrSize, filename);
                System.out.println(line);
                writer.write(line + "\r\n");
            }
            writer.flush();
            writer.close();
            sendMSGToClient("226 Transfer complete.");
            System.out.println("226 Transfer complete.");
            dataSocket.close();
        } else {
            sendMSGToClient("550 Empty Directory.");
        }
    }

    /**
     *
     * @param dir List all names of files and directory under dir
     * @throws IOException
     */
    private void handleNLST(String dir) throws IOException {
        if(dataSocket == null) {
            sendMSGToClient("550 PASV Mode needed first.");
            return;
        }

        File[] files = new File(dir).listFiles();
        if(files != null) {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(dataSocket.getOutputStream(), StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter);
            sendMSGToClient("125 Data connection already open; Transfer starting.");
            for (File file : files) {
                String filename = file.getName();
                writer.write(filename + "\r\n");
            }
            writer.flush();
            writer.close();
            sendMSGToClient("226 Transfer complete.");
            System.out.println("226 Transfer complete.");
            dataSocket.close();
        } else {
            sendMSGToClient("550 Empty Directory.");
        }
    }

    /**
     *
     * @param type Transferring Type. ASCII: text, BIN: binary file except text
     * @throws IOException
     */
    private void handleTYPE(String type) throws IOException{
        if(type.equals("A"))
            this.type = TYPE.ASCII;
        else if(type.equals("I"))
            this.type = TYPE.BIN;
        else
            sendMSGToClient("550 Unknown transfer type. Only Type: A/I available.");
        sendMSGToClient("200 Change mode into TYPE " + type);
        System.out.println("Type: " + this.type);
    }

    private void handleRETR(String filename) throws IOException{
        ArrayList<String> filenames = getPWDFilenames();
        if(!filenames.contains(filename)) {
            sendMSGToClient("550-The system cannot find the file specified.");
            return;
        }
        if(type == TYPE.BIN) {
            sendMSGToClient("125 Data transfer starts.");
            OutputStream outputStream = dataSocket.getOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(outputStream);
            FileInputStream inputStream = new FileInputStream(new File(filename));
            BufferedInputStream input = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[bufferSize];
            while (input.read(buffer) > 0) {
                out.write(buffer);
            }
            out.flush();
            out.close();
            input.close();
            dataSocket.close();
            sendMSGToClient("226 Transfer complete.");
        }
        if(type == TYPE.ASCII) {
            sendMSGToClient("125 Data transfer starts.");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(dataSocket.getOutputStream());
            BufferedWriter out = new BufferedWriter(outputStreamWriter);

            FileInputStream inputStream = new FileInputStream(new File(filename));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader input = new BufferedReader(inputStreamReader);

            String line = null;
            while((line = input.readLine()) != null) {
                System.out.println(line);
                out.write(line+"\r\n");
            }
            out.flush();
            out.close();
            input.close();
            dataSocket.close();
            sendMSGToClient("226 Transfer complete.");
        }
    }

    private void sendMSGToClient(String msg) throws IOException{
        writer.write(msg+"\r\n");
        writer.flush();
    }

    private boolean isLoggedIn() throws IOException{
        if(currentStatus != userStatus.LOGGEDIN) {
            sendMSGToClient("530 You have not logged in!");
            return false;
        }
        return true;
    }

    private ArrayList<String> getPWDFilenames() {
        File[] files = new File(currentDir).listFiles();
        ArrayList<String> filenames = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                filenames.add(f.getName());
            }
        }
        return filenames;
    }

    private void debugOutput(String str) {
        System.out.println(str);
    }
}