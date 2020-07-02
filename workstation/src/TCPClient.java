import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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

        Channel(s);
        //释放
        s.close();
    }

    public static void Channel(Socket s) throws IOException, InterruptedException {
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
            } else if (message.equals("user block")) {
                System.out.println("> Your account has been blocked due to multiple login failures. Please try again later");
                userIDFlag = 0;
                Thread.sleep(10000);
            } else {
                System.out.println("> Invalid username. Please try again");
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
                System.out.println("> Welcome to the BlueTrace Simulator");
                loginFlag = 1;
                loginAttempt = 0;
            } else {
                loginAttempt = dis.readInt();
                String message = "> Invalid password. You can still attempt " + (3 - loginAttempt);
                String time = (loginAttempt == 2) ? " time." : " times.";
                message = message + time;
                if (loginAttempt < 3) {
                    System.out.println(message);
                } else {
                    System.out.println("> Invalid password. Your account has been blocked. Please try again later");
                    Thread.sleep(10000);
                }
                loginFlag = 0;
            }
        } while (loginFlag == 0);

        do {
            System.out.print("> ");
            String command = console.readLine();
            dos.writeUTF(command);
            dos.flush();
            if (command.equals("Download_tempID")) {
                String response = dis.readUTF();
                System.out.println("tempID:" + "\n\r" + response);
            } else if (command.equals("logout")) {
                dos.close();
                dis.close();
                s.close();
                break;
            } else if (command.equals("Upload_contact_log")) {
                contactLog.upload(dos);
            } else {
                System.out.println("> Error. Invalid command");
            }
            //持续保持接受命令状态，可通过不同的命令来指向下一个function
        } while (true);
    }
}

class contactLog {

    public static void upload(DataOutputStream dos) throws IOException {
        String pathname = "z5142012_contactlog.txt";
        int lineNum = 0;
        ArrayList<String> content = new ArrayList<>();
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                content.add(line);
                System.out.println(line);
                lineNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        dos.writeInt(lineNum);
        dos.flush();
        for (int i = 0; i < lineNum; i++) {
            dos.writeUTF(content.get(i));
            dos.flush();
        }
    }
}
