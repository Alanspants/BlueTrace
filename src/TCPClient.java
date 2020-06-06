import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClient {
    public static void main(String[] args) throws IOException {
        System.out.println(">>> TCP Client start ...");

        String server_ip = args[0];
        int server_port = Integer.parseInt(args[1]);
        int client_udp_port = Integer.parseInt(args[2]);
        System.out.println("> Server_ip: " + server_ip);
        System.out.println("> Server_port: " + server_port);
        System.out.println("> Client_udp_port: " + client_udp_port);
        System.out.println("----------------- TCPClient -----------------");

        Client(server_ip, server_port);
    }

    public static void Client(String server_ip, int server_port) throws IOException {
        //创建Socket对象
        Socket s = new Socket(InetAddress.getByName(server_ip), server_port);

        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        DataInputStream dis = new DataInputStream(s.getInputStream());

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("> UserID: ");
        String UserID = console.readLine();
        System.out.print("> Password: ");
        String password = console.readLine();

        dos.writeUTF(UserID + " " + password);
        dos.flush();

        String datas = dis.readUTF();
        System.out.println("> " + datas);

        //释放
        s.close();
    }

}

