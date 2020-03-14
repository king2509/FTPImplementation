import java.io.IOException;
import java.util.ArrayList;
/*
1. 判断文件/目录是否存在  √
2. 若目录存在，则不再新建目录  √
3. 若文件存在，询问是否更新文件 √

1. 删除文件/目录前，查看该文件/目录是否存在
*/

public class Client {
    public static void main(String[] args) throws IOException {
        MyFTPClient myFTPClient = new MyFTPClient();
        myFTPClient.connect("192.168.73.1");
        myFTPClient.login("king","0925");
        myFTPClient.getFileSize("king.txt");
        ArrayList<FTPFile> res = myFTPClient.getList("upload");
        for (FTPFile re : res) {
            System.out.println(re.toString());
        }
        myFTPClient.close();
    }
}
