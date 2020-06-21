import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class TCPServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(">>> TCP Server start ...");

        int server_port = Integer.parseInt(args[0]);
        int block_duration = Integer.parseInt(args[1]);
        System.out.println("> server_port: " + server_port);
        System.out.println("> block_duration: " + block_duration);
        System.out.println("----------------- TCPServer -----------------");
        Server(server_port);
    }

    public static void Server(int server_port) throws IOException, InterruptedException {

        // Create ServerSocket object
        ServerSocket ss = new ServerSocket(server_port);

        // User Authorization
        userAuthorize(ss);
    }

    public static void userAuthorize(ServerSocket ss) throws IOException, InterruptedException {
        // Listening
        Socket s = ss.accept();
        // Set stream
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        // Set credentials hashMap
        HashMap<String, String> Credentials = readCredentials();
        HashMap<String, Integer> loginAttempt = createLoginAttempt();

        String userID = "";
        int userIDFlag = 0;
        do {
            userID = dis.readUTF();
            System.out.println("> Received userID: " + userID);
            if (Credentials.containsKey(userID)) {
                dos.writeUTF("userID existed");
                dos.flush();
                System.out.println("> userID existed, wait for password input...");
                userIDFlag = 1;
            } else {
                dos.writeUTF("userID wrong");
                dos.flush();
                System.out.println("> UserID wrong, wait for another try...");
                userIDFlag = 0;
            }
        } while (userIDFlag == 0);

        String password = "";
        int loginFlag = 0;
        do {
            if (loginAttempt.get(userID) == 3) {
//                Thread.sleep(5000);
                loginAttempt.put(userID, 0);
            }
            password = dis.readUTF();
            System.out.println("> Received password: " + password);
            if (Credentials.get(userID).equals(password)) {
                loginAttempt.put(userID, 0);
                dos.writeUTF("password collect");
                dos.flush();
                System.out.println("----------------- Authorized success, " + userID + " -----------------");
                loginFlag = 1;
            } else {
                loginAttempt.put(userID, loginAttempt.get(userID) + 1);
                dos.writeUTF("password wrong");
                dos.write(loginAttempt.get(userID));
                dos.flush();
                String message = "> password wrong, user can attempt " + (3 - loginAttempt.get(userID)) + " times, wait for another try...";
                System.out.println(message);
                loginFlag = 0;
            }
        } while (loginFlag == 0);

    }

    public static HashMap<String, String> readCredentials() throws IOException {
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

    public static HashMap<String, Integer> createLoginAttempt() {
        HashMap<String, Integer> loginAttempt = new HashMap<>();
        String pathname = "credentials.txt";
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            // Read file line to line
            String line;
            while ((line = br.readLine()) != null) {
                // Regex data to get UserID
                String[] dataArray = line.split(" ");
                String UserID = dataArray[0];
                // Put data into hashMap
                loginAttempt.put(UserID, 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loginAttempt;
    }

}

