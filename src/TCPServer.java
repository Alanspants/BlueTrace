import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class TCPServer {
    public static void main(String[] args) throws IOException {
        System.out.println(">>> TCP Server start ...");

        int server_port = Integer.parseInt(args[0]);
        int block_duration = Integer.parseInt(args[1]);
        System.out.println("> server_port: " + server_port);
        System.out.println("> block_duration: " + block_duration);
        System.out.println("----------------- TCPServer -----------------");

        Server(server_port);
    }

    public static void Server(int server_port) throws IOException {
        //创建Socket对象
        ServerSocket ss = new ServerSocket(server_port);

        //监听（阻塞）
        Socket s = ss.accept();

        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        //获取数据
        String datas = dis.readUTF();
        String[] dataArray = datas.split(" ");
        String UserID = dataArray[0];
        String password = dataArray[1];

        //输出数据
        InetAddress address = s.getInetAddress();
        System.out.println("> Received UserID: " + UserID + "\n" + "> Received password: " + password);

        //返回数据
        dos.writeUTF("Authorized");
        dos.flush();
        System.out.println("> Authorized");

        //释放
        s.close();
    }
}

