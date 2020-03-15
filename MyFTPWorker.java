import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

class MyFTPWorker extends Thread{

    private String USER = "king";
    private String PASS = "0925";

    private Socket controlSocket = null;
    private BufferedWriter writer = null;
    private boolean quitCMDLoop = false;

    private enum userStatus {ENTEREDUSER, LOGGEDIN, NOTLOGGEDIN};
    private userStatus currentStatus;

    private BufferedReader reader = null;
    private Socket dataSocket = null;
    private int dataPort;

    // file dir
    private String root;
    private String currentDir;


    MyFTPWorker(Socket socket, int dataPort) throws IOException {
        this.controlSocket = socket;
        root = System.getProperty("user.dir");
        currentDir = root;
        this.dataPort = dataPort;
    }
    public void run() {
        System.out.println("Thread starts to run!");
        try {
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));

            sendMSGToClient("220 FTP server opens!");

            while(!quitCMDLoop) {
                executeCMD(reader.readLine());
            }
        } catch (IOException e){
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



    private void executeCMD(String request) {
        String[] requestInfo = request.split(" ");
        String cmd = requestInfo[0].toUpperCase();
        String args = "";
        if(requestInfo.length > 1)
            args = requestInfo[1].toUpperCase();
        try {
            switch (cmd) {
                case "USER": {
                    handleUSER(args);
                    break;
                }
                case "PASS": {
                    handlePASS(args);
                    break;
                }

                case "PWD":{
                    handlePWD();
                    break;
                }

                case "CWD":{
                    handleCWD(args);
                    break;
                }

                case "QUIT": {
                    handleQUIT();
                    break;
                }
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
        } else if(username.toLowerCase().equals(USER)) {
            currentStatus = userStatus.ENTEREDUSER;
            sendMSGToClient("331 PASS required");
        } else {
            sendMSGToClient("530 wrong username");
        }
    }

    private void handlePASS(String pass) throws IOException{
        if(currentStatus == userStatus.LOGGEDIN) {
            sendMSGToClient("530 user has logged in");
        } else if(currentStatus != userStatus.ENTEREDUSER) {
            sendMSGToClient("530 username required first");
        } else {
            sendMSGToClient("230 Logged in successfully");
            currentStatus = userStatus.LOGGEDIN;
        }
    }

    private void handlePWD() throws IOException {
        sendMSGToClient(String.format("%d \"%s\" is current directory",257,currentDir));
    }

    private boolean handleCWD(String dir) throws IOException {
        File[] files = new File(root).listFiles();
        ArrayList<String> filenames = new ArrayList<>();
        if(files != null) {
            for(File f: files) {
                filenames.add(f.getName());
            }
        }
        if(!filenames.contains(dir)) {
            sendMSGToClient("550 No such directory");
            return false;
        } else {
            currentDir = currentDir + "/" + dir;
        }
        sendMSGToClient("257 current directory: "+ currentDir);
        return true;
    }

    private void handleQUIT() throws IOException{
        sendMSGToClient("221 The connection closed");
        quitCMDLoop = true;
    }

    private void sendMSGToClient(String msg) throws IOException{
        writer.write(msg+"\r\n");
        writer.flush();
    }

    private void debugOutput(String str) {
        System.out.println(str);
    }
}