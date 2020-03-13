import java.io.IOException;
/*
1. 判断文件/目录是否存在  √
2. 若目录存在，则不再新建目录  √
3. 若文件存在，询问是否更新文件 √
*/


public class Client {

    public static void main(String[] args) throws IOException {
        MyFTPClient myFTPClient = new MyFTPClient();
        myFTPClient.connect("192.168.73.1");
        myFTPClient.login("king","0925");
        myFTPClient.uploadFile("king.mp4");
        myFTPClient.uploadFile("king.mp4");
        //myFTPClient.mkdir("upload");
        //myFTPClient.toDir("upload");
        //myFTPClient.stor("king.txt");
        myFTPClient.close();
    }
}
