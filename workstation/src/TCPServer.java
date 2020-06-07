import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
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

        // Create ServerSocket object
        ServerSocket ss = new ServerSocket(server_port);

        // User Authorization
        userAuthorize(ss);
    }

    public static void userAuthorize(ServerSocket ss) throws IOException {
        // Listening
        Socket s = ss.accept();

        // Set stream
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        int loginFlag = 0;
        do {
            // Receive UserID and password input by client
            String datas = dis.readUTF();
            String[] dataArray = datas.split(" ");
            String UserID = dataArray[0];
            String password = dataArray[1];
            InetAddress address = s.getInetAddress();
            System.out.println("> Received UserID: " + UserID + "\n" + "> Received password: " + password);

            // Get User data from Credentials.txt
            HashMap Credentials = readCredentials();
            if (Credentials.containsKey(UserID)) {
                // Registered user
                if (Credentials.get(UserID).equals(password)) {
                    // Right password
                    dos.writeInt(1);
                    dos.flush();
                    System.out.println("> Authorized");
                    loginFlag = 1;
                } else {
                    // Wrong password
                    dos.writeInt(0);
                    dos.flush();
                    System.out.println("> Wrong password, wait for another try");
                    loginFlag = 0;
                }
            } else {
                // New User
                dos.writeInt(1);
                dos.flush();
                loginFlag = 1;
                System.out.println("> Register success");
            }
        } while (loginFlag != 1);

        // Release socket
        s.close();
    }

    public static HashMap readCredentials() throws IOException {
        HashMap<String, String> credentials = new HashMap<>();
        String pathname = "credentials.txt";
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            // Read file line to line
            String line;
            while ((line = br.readLine()) != null) {
                // Regex data to get UserID and password
                String[] dataArray = line.split(" ");
                String UserID = dataArray[0];
                String password = dataArray[1];
                // Put data into hashMap
                credentials.put(UserID, password);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return credentials;
    }
}

