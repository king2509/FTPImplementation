import javax.imageio.stream.ImageOutputStreamImpl;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

public class MyFTPClient {
    private boolean DEBUG = true;
    private Socket socket = null;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;
    private String response = null;
    private final int bufferSize = 4096;

    MyFTPClient(){

    }

    synchronized void connect(String ip) throws IOException{
        connect(ip, 21);
    }

    synchronized void connect(String ip, int port) throws IOException {
        // judge if socket exists
        if(socket != null) {
            throw new IOException("socket has existed! Disconnect first.");
        }
        socket = new Socket(ip, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        response = readLine();
        if(!response.startsWith("220")) {
            throw new IOException(response);
        }
        System.out.println("Connected Successfully");
    }

    synchronized void login(String username, String password) throws IOException{
        if(socket == null)
            throw new IOException("Connect First");
        sendLine("USER "+username);
        response = readLine();
        if(!response.startsWith("331")) {
            throw new IOException(response);
        }
        sendLine("PASS "+password);
        response = readLine();
        if(!response.startsWith("230")) {
            throw new IOException(response);
        }
        System.out.println("Logged successfully");
    }

    // Set PASV mode & Establish a dataSocket
    private synchronized Socket getDataSocket() throws IOException{
        String remoteIp = null;
        int remotePort = -1;
        sendLine("PASV");
        response = readLine();
        if(!response.startsWith("227")) {
            throw new IOException(response);
        }
        // response of "PASV": (x,x,x,x,y1,y2)
        // (x.x.x.x.y1.y2)  ip:x.x.x.x  port:y1*256+y2
        int opening = response.indexOf('(');
        int closing = response.indexOf(')');
        if(closing > 0) {
            String ipAndPort = response.substring(opening + 1, closing);
            StringTokenizer dataLink = new StringTokenizer(ipAndPort, ",");
            try{
                remoteIp = dataLink.nextToken() + "." + dataLink.nextToken()
                        + "." + dataLink.nextToken() + "." + dataLink.nextToken();
                remotePort = Integer.parseInt(dataLink.nextToken()) * 256 + Integer.parseInt(dataLink.nextToken());
            } catch (Exception e) {
                throw new IOException("DataLink broken");
            }
        }
        return new Socket(remoteIp, remotePort);
    }


    // change working directory
    synchronized boolean toDir(String dir) throws IOException{
        sendLine("CWD "+ dir);
        response = readLine();
        System.out.println(response);
        if(!response.startsWith("250"))
            throw new IOException(response);
        return true;
    }


    // return current working directory
    synchronized String pwd() throws IOException{
        sendLine("PWD");
        response = readLine();
        if(!response.startsWith("257"))
            throw new IOException(response);
        int firstIndex = response.indexOf('"');
        int secondIndex = response.indexOf('"', firstIndex+1);
        return response.substring(firstIndex+1, secondIndex);
    }

    void close() throws IOException{
        try{
            sendLine("QUIT");
        } catch (Exception e) {
            e.getStackTrace();
        }
        if(socket != null)
            socket.close();
        System.out.println("Connection closed");
    }

    // Transfer file to remote server
    boolean stor(String filename) throws IOException{
        File file = new File(filename);
        if(file.isDirectory())
            throw new IOException("MyFTPClient cannot upload a directory");
        Socket dataSocket = getDataSocket();
        FileInputStream inputStream = new FileInputStream(file);
        BufferedInputStream input = new BufferedInputStream(inputStream);
        sendLine("STOR " + filename);
        response = readLine();
        BufferedOutputStream output = new BufferedOutputStream(dataSocket.getOutputStream());
        long fileSize = file.length();
        System.out.println("fileSize: " + fileSize);
        long cnt = 0;
        byte[] buffer = new byte[bufferSize];
        while(input.read(buffer) > 0) {
            output.write(buffer);
            cnt++;
            System.out.print(String.format("Transferred: %d / %d\r", cnt*bufferSize ,fileSize));
        }
        output.flush();
        output.close();
        input.close();
        dataSocket.close();
        System.out.println(String.format("Transferred: %d / %d", fileSize ,fileSize));
        response = readLine();
        return response.startsWith("226 ");
    }

    // cd upper directory
    // if pwd==root, cdup success as well.
    synchronized boolean toUpperDir() throws IOException{
        sendLine("CDUP");
        response = readLine();
        return response.startsWith("250");
    }

    synchronized boolean renameFile(String oldFilename, String newFilename) throws IOException{
        sendLine("RNFR " + oldFilename);
        response = readLine();
        if(!response.startsWith("350"))
            throw new IOException("No target file");
        sendLine("RNTO " + newFilename);
        response = readLine();
        return response.startsWith("250");
    }

    // mkdir  needs to check if directory exists
    synchronized boolean mkdir(String directoryName) throws IOException{
        sendLine("MKD "+ directoryName);
        response = readLine();
        if(!response.startsWith("257"))
            throw new IOException(response);
        return response.startsWith("257");
    }

    // list all names of files & directories under directory specified
    synchronized boolean getNLST(String dir) throws IOException{
        Socket dataSocket = getDataSocket();
        sendLine("NLST " + dir);
        response = readLine();
        if(!response.startsWith("125"))
            throw new IOException(response);
        InputStreamReader inputStreamReader = new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
        }
        response = readLine();
        dataSocket.close();
        if(!response.startsWith("226"))
            throw new IOException(response);
        return response.startsWith("226");
    }

    // list details of all files and directories under directory specified
    synchronized boolean getList(String dir) throws IOException{
        Socket dataSocket = getDataSocket();
        sendLine("LIST " + dir);
        response = readLine();
        if(!response.startsWith("125"))
            throw new IOException(response);
        InputStreamReader inputStreamReader = new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
        }
        response = readLine();
        dataSocket.close();
        if(!response.startsWith("226"))
            throw new IOException(response);
        return true;
    }

    long getFileSize(String filename) throws IOException{
        sendLine("SIZE " + filename);
        response = readLine();
        System.out.println(response);
        if(!response.startsWith("213"))
            throw new IOException(response);
        StringTokenizer stringTokenizer = new StringTokenizer(response);
        stringTokenizer.nextToken();
        return Long.parseLong(stringTokenizer.nextToken());
    }

    // Retrieve file specified
    boolean retrieveAsciiFile(String filename) throws IOException{
        Socket dataSocket = getDataSocket();
        // fileSize method should be called before RETR
        long fileSize = getFileSize(filename);
        ascii();
        sendLine("RETR " + filename);
        response = readLine();
        if(!response.startsWith("125"))
            throw new IOException(response);
        BufferedInputStream in = new BufferedInputStream(dataSocket.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
        byte[] buffer = new byte[bufferSize];
        long cnt = 1;
        while(in.read(buffer) != -1) {
            out.write(buffer);
            cnt++;
            System.out.print(String.format("Transferred: %d / %d\r", cnt*bufferSize ,fileSize));
        }
        out.flush();
        out.close();
        System.out.println(String.format("Transferred: %d / %d", fileSize ,fileSize));
        response = readLine();
        System.out.println(response);
        in.close();
        return true;
    }

    boolean retrieveBinaryFile(String filename) throws IOException{
        Socket dataSocket = getDataSocket();
        // get fileSize first
        long fileSize = getFileSize(filename);
        bin();
        sendLine("RETR " + filename);
        response = readLine();
        if(!response.startsWith("125"))
            throw new IOException(response);

        BufferedInputStream in = new BufferedInputStream(dataSocket.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
        byte[] buffer = new byte[bufferSize];
        long cnt = 1;
        while(in.read(buffer) != -1) {
            out.write(buffer);
            cnt++;
            System.out.print(String.format("Transferred: %d / %d\r", cnt*bufferSize ,fileSize));
        }
        out.flush();
        System.out.println(String.format("Transferred: %d / %d", fileSize ,fileSize));
        response = readLine();
        System.out.println(response);
        out.close();
        in.close();
        return true;
    }

    // Delete file specified
    synchronized boolean deleteFile(String filename) throws IOException{
        sendLine("DELE " + filename);
        response = readLine();
        if(response.startsWith("550"))
            throw new IOException("Cannot find file specified");
        return response.startsWith("250");
    }

    // Delete empty directory specified, cannot delete an nonempty directory
    synchronized boolean deleteDir(String dir) throws IOException{
        sendLine("RMD " + dir);
        response = readLine();
        if(!response.startsWith("250"))
            throw new IOException(response);
        return true;
    }

    // Mode binary: mp4, jpg, ...
    private synchronized boolean bin() throws IOException{
        sendLine("TYPE A");
        String response = readLine();
        return response.startsWith("200");
    }

    // Mode ascii: txt
    private synchronized boolean ascii() throws IOException{
        sendLine("TYPE I");
        String response = readLine();
        return response.startsWith("200");
    }

    // read response from server
    private String readLine() throws IOException{
        if(socket == null)
            throw new IOException("socket needs to be connected");
        String line = reader.readLine();
        if(DEBUG)
            System.out.println("< RESPONSE: "+line);
        return line;
    }

    // send request to server
    private void sendLine(String cmd) throws IOException{
        if(socket == null)
            throw new IOException("socket needs to be connected");
        try{
            writer.write(cmd+"\r\n");
            writer.flush();
            if(DEBUG) {
                System.out.println("< CMD: " + cmd);
            }
        } catch (IOException e) {
            socket = null;
            throw e;
        }
    }

}


