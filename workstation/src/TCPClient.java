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

        String userID = "";
        int userIDFlag = 0;
        do {
            System.out.print("> userID: ");
            userID = console.readLine();
            dos.writeUTF(userID);
            dos.flush();

            String message = dis.readUTF();
            if (message.equals("userID existed")) {
                userIDFlag = 1;
            } else if (message.equals("already login")) {
                System.out.println("> User already login or is processing login in other terminal");
                userIDFlag = 0;
            } else {
                System.out.println("> UserID didn't exist, please try again");
                userIDFlag = 0;
            }
        } while (userIDFlag == 0);

        String password = "";
        int loginFlag = 0;
        int loginAttempt = 0;
        do {
            System.out.print("> password: ");
            password = console.readLine();
            dos.writeUTF(password);
            dos.flush();
            if (dis.readUTF().equals("password collect")) {
                System.out.println("----------------- Welcome back, " + userID + " -----------------");
                loginFlag = 1;
                loginAttempt = 0;
            } else {
                loginAttempt = dis.read();
                String message = "> password wrong, you can attempt " + (3 - loginAttempt) + " times, wait for another try...";
                if (loginAttempt < 3) {
                    System.out.println(message);
                } else {
                    System.out.println("Three consecutive failed attempts, you will be blocked 5s");
                    Thread.sleep(10000);
                }
                loginFlag = 0;
            }
        } while (loginFlag == 0);

        do{
            String command = console.readLine();
            dos.writeUTF(command);
            dos.flush();
            //持续保持接受命令状态，可通过不同的命令来指向下一个function
        }while(true);
    }
}
