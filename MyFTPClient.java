import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;

public class MyFTPClient {
    private boolean DEBUG = true;
    private Socket socket = null;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    public MyFTPClient(){

    }

    private synchronized void connect() throws IOException{
        connect("192.168.73.1", 21);
    }

    private synchronized void connect(String ip, int port) throws IOException {
        // judge if socket exists
        if(socket != null) {
            throw new IOException("socket has existed! Disconnect first.");
        }
        socket = new Socket(ip, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        String response = readLine();
        if(!response.startsWith("220")) {
            throw new IOException(
                    "MyFTPClient received a unknown response: "
                            +response);
        }
        System.out.println("Connected Successfully");
    }

    private synchronized void login(String username, String password) throws IOException{
        if(socket == null)
            throw new IOException("Connect First");
        sendLine("USER "+username);
        String response = readLine();
        if(!response.startsWith("331 ")) {
            throw new IOException(
                    "MyFTPClient received a unknown response after sending USERNAME "
                            + response);
        }
        sendLine("PASS "+password);
        response = readLine();
        if(!response.startsWith("230 ")) {
            throw new IOException(
                    "MyFTPClient received a unknown response after sending PASSWORD "
                            + response);
        }
        System.out.println("Logged successfully");
    }

    // change working directory
    private synchronized boolean cwd(String dir) throws IOException{
        sendLine("CWD "+ dir);
        String response = readLine();
        System.out.println(response);
        if(response.startsWith("550"))
            throw new IOException("The system cannot find the file specified");
        return response.startsWith("250");
    }


    // return current working directory
    private synchronized String pwd() throws IOException{
        sendLine("PWD");
        String dir = readLine();
        if(!dir.startsWith("257"))
            throw new IOException("MyFTPCLient reveived a unknown response after sending PWD");
        int firstIndex = dir.indexOf('"');
        int secondIndex = dir.indexOf('"', firstIndex+1);
        return dir.substring(firstIndex+1, secondIndex);
    }

    private void close() throws IOException{
        try{
            sendLine("QUIT");
        } catch (Exception e) {
            e.getStackTrace();
        }
        if(socket != null)
            socket.close();
        if(DEBUG)
            System.out.println("Connection closed");
    }

    // Transfer file to remote server
    private boolean stor(File file) throws IOException{
        if(file.isDirectory())
            throw new IOException("MyFTPClient cannot upload a directory");
        String filename = file.getName();
        FileInputStream inputStream = new FileInputStream(file);
        BufferedInputStream input = new BufferedInputStream(inputStream);
        sendLine("PASV");
        String response = readLine();
        if(!response.startsWith("227")) {
            throw new IOException("Server could not get into PASSIVE MODE");
        }
        String ip = null;
        int port = -1;
        int opening = response.indexOf('(');
        int closing = response.indexOf(')');
        if(closing > 0) {
            String ipAndPort = response.substring(opening + 1, closing);
            StringTokenizer dataLink = new StringTokenizer(ipAndPort, ",");
            try{
                ip = dataLink.nextToken() + "." + dataLink.nextToken()
                        + "." + dataLink.nextToken() + "." + dataLink.nextToken();
                port = Integer.parseInt(dataLink.nextToken()) * 256 + Integer.parseInt(dataLink.nextToken());
            } catch (Exception e) {
                throw new IOException("DataLink broken");
            }
        }

        sendLine("STOR " + filename);
        Socket dataSocket = new Socket(ip, port);
        response = readLine();
        if(!response.startsWith("150"))
            throw new IOException("Failed to store file");
        BufferedOutputStream output = new BufferedOutputStream(dataSocket.getOutputStream());
        long fileSize = file.length() / 4096;
        System.out.println("fileSize: " + fileSize);
        long cnt = 0;
        byte[] buffer = new byte[4096];
        while(input.read(buffer) > 0) {
            output.write(buffer);
            cnt++;
            System.out.print(String.format("Transferred: %d%s\r",cnt*100/fileSize, "%"));
        }
        output.flush();
        output.close();
        input.close();
        response = readLine();
        return response.startsWith("226 ");
    }

    private synchronized boolean bin() throws IOException{
        sendLine("TYPE A");
        String response = readLine();
        return response.startsWith("200");
    }

    private synchronized boolean ascii() throws IOException{
        sendLine("TYPE I");
        String response = readLine();
        return response.startsWith("200");
    }

    private String readLine() throws IOException{
        if(socket == null)
            throw new IOException("socket needs to be connected");
        String line = reader.readLine();
        if(DEBUG)
            System.out.println("< "+line);
        return line;
    }

    private void sendLine(String cmd) throws IOException{
        if(socket == null)
            throw new IOException("socket needs to be connected");
        try{
            writer.write(cmd+"\r\n");
            writer.flush();
            if(DEBUG) {
                System.out.println(cmd);
            }
        } catch (IOException e) {
            socket = null;
            throw e;
        }
    }

    public static void main(String[] args) throws IOException {
        MyFTPClient myFTPClient = new MyFTPClient();
        myFTPClient.connect();
        myFTPClient.login("king","0925");
        //myFTPClient.bin();
        myFTPClient.ascii();
        myFTPClient.stor(new File("D:/king.mp4"));
        myFTPClient.close();
    }
}


