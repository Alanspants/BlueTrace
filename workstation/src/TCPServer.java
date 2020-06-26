import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class TCPServer {
    static HashMap<String, Integer> loginAttempt = null;
    static HashMap<String, String> Credentials = null;
    static HashMap<String, Boolean> alreadyLogin = null;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(">>> TCP Server start ...");
        int server_port = Integer.parseInt(args[0]);
        int block_duration = Integer.parseInt(args[1]);
        System.out.println("> server_port: " + server_port);
        System.out.println("> block_duration: " + block_duration);
        System.out.println("----------------- TCPServer -----------------");

        try {
            Credentials = readCredentials();
            loginAttempt = createLoginAttempt();
            alreadyLogin = createAlreadyLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Server(server_port);
    }

    public static void Server(int server_port) throws IOException, InterruptedException {
        ServerSocket ss = new ServerSocket(server_port);
        boolean status = true;
        while (status) {
            Socket client = ss.accept();
            new Thread(new userAuthorize(client)).start();
        }
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

    public static HashMap<String, Boolean> createAlreadyLogin() {
        HashMap<String, Boolean> alreadyLogin = new HashMap<>();
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
                alreadyLogin.put(UserID, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return alreadyLogin;
    }
}


class userAuthorize implements Runnable {

    Socket client;

    public userAuthorize(Socket client) {
        this.client = client;
    }

    public void run() {
        String userID = "";
        try {
            DataInputStream dis = new DataInputStream(client.getInputStream());
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());

            int userIDFlag = 0;
            do {
                userID = dis.readUTF();
                if (TCPServer.Credentials.containsKey(userID)) {
                    if (TCPServer.loginAttempt.get(userID) == 3){
                        dos.writeUTF("user block");
                        dos.flush();
                        Thread.sleep(10000);
                        TCPServer.loginAttempt.put(userID, 0);
                        TCPServer.alreadyLogin.put(userID, false);
                        continue;
                    }
                    if (TCPServer.alreadyLogin.get(userID)) {
                        dos.writeUTF("already login");
                        dos.flush();
                        userIDFlag = 0;
                    } else {
                        TCPServer.alreadyLogin.put(userID, true);
                        dos.writeUTF("userID existed");
                        dos.flush();
                        userIDFlag = 1;
                    }
                } else if (!TCPServer.Credentials.containsKey(userID)) {
                    dos.writeUTF("userID wrong");
                    dos.flush();
                    userIDFlag = 0;
                }
            } while (userIDFlag == 0);


            String password = "";
            int loginFlag = 0;
            do {
//                if (TCPServer.loginAttempt.get(userID) == 3) {
//                    TCPServer.loginAttempt.put(userID, 0);
//                }
                password = dis.readUTF();
                if (TCPServer.Credentials.get(userID).equals(password)) {
                    TCPServer.loginAttempt.put(userID, 0);
                    dos.writeUTF("password collect");
                    dos.flush();
                    System.out.println("> " + userID + " login successfully");
                    loginFlag = 1;
                } else {
                    TCPServer.loginAttempt.put(userID, TCPServer.loginAttempt.get(userID) + 1);
                    dos.writeUTF("password wrong");
                    dos.writeInt(TCPServer.loginAttempt.get(userID));
                    dos.flush();
                    loginFlag = 0;
                    if(TCPServer.loginAttempt.get(userID) == 3){
                        Thread.sleep(10000);
                        TCPServer.loginAttempt.put(userID, 0);
                    }
                }
            } while (loginFlag == 0);
            do {
                String command = dis.readUTF();
                //持续从client来接受命令，如果中途control+c退出则视为退出当前account
            } while (true);
        } catch (IOException | InterruptedException e) {
            if (TCPServer.alreadyLogin.containsKey(userID) && TCPServer.alreadyLogin.get(userID)) {
                TCPServer.alreadyLogin.put(userID, false);
            }
//            e.printStackTrace();
            if (userID != "") {
                System.out.println("> Client program terminated by " + userID);
            }
        }
//        TCPServer.alreadyLogin.put(userID, false);
    }


}

