import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MyFTPSever {
    private static int controlPort = 3000;
    private int numOfDataPort = 0;
    public static void main(String[] args) throws IOException {
        new MyFTPSever();
    }

    public MyFTPSever() throws IOException{
        System.out.println("FTP Sever open!");
        ServerSocket serverSocket = new ServerSocket(controlPort);
        while(true) {
            Socket socket = serverSocket.accept();
            MyFTPWorker w = new MyFTPWorker(socket);
            System.out.println("socket: " + socket.getPort());
            w.start();
        }
    }
}