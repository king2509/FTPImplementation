import java.io.IOException;

public class Client {

    public static void main(String[] args) throws IOException {
        MyFTPClient myFTPClient = new MyFTPClient();
        myFTPClient.connect("192.168.73.1");
        myFTPClient.login("king","0925");
        myFTPClient.mkdir("upload");
        myFTPClient.toDir("upload");
        myFTPClient.stor("king.txt");
        myFTPClient.close();
    }
}
