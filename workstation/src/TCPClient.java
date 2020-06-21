import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {
    public static void main(String[] args) throws IOException, InterruptedException {
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

    public static void Client(String server_ip, int server_port) throws IOException, InterruptedException {
        //创建Socket对象
        Socket s = new Socket(InetAddress.getByName(server_ip), server_port);

        userLogin(s);

        //释放
        s.close();
    }

    public static void userLogin(Socket s) throws IOException, InterruptedException {
        // Set stream
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        DataInputStream dis = new DataInputStream(s.getInputStream());
        // Set reader
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        int login_responds = 0;
        String UserID = "";
        String password = "";
        int attempt = 0;
        do {
            // Read data from console
            System.out.print("> UserID: ");
            UserID = console.readLine();
            System.out.print("> Password: ");
            password = console.readLine();

            // Output data to Server
            dos.writeUTF(UserID + " " + password);
            dos.flush();

            // Get responds from Server
            login_responds = dis.readInt();
            if (login_responds == 0) {
                attempt += 1;
                if (attempt == 3) {
                    System.out.println("Wrong password, attempt will be blocked for 60s!");
                    Thread.sleep(10000);
                    attempt = 0;
                } else {
                    System.out.println("> Wrong password, you can still attempt " + (3 - attempt) + "times");
                }
            }
        } while (login_responds == 0);

        if (login_responds == 2) {
            System.out.println("> Register Success");
            System.out.println("----------------- Welcome, " + UserID + " -----------------");
        } else {
            System.out.println("> Login Success");
            System.out.println("----------------- Welcome back, " + UserID + " -----------------");
        }
    }

}

