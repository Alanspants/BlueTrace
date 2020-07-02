import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
            new Thread(new Channel(client)).start();
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


class Channel implements Runnable {

    Socket client;

    public Channel(Socket client) {
        this.client = client;
    }

    public void run() {
        String userID = "";
        try {
            // Set DataInputStream and DataOutputStream
            DataInputStream dis = new DataInputStream(client.getInputStream());
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            int userIDFlag = 0;
            // Username validation section
            do {
                userID = dis.readUTF();
                // Valid username
                if (TCPServer.Credentials.containsKey(userID)) {
                    // If this user is being blocked in other window
                    if (TCPServer.loginAttempt.get(userID) == 3) {
                        dos.writeUTF("user block");
                        dos.flush();
                        // Reset the block period
                        Thread.sleep(10000);
                        TCPServer.loginAttempt.put(userID, 0);
                        continue;
                    }
                    // If the user has already login
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
                    // Invalid username
                } else if (!TCPServer.Credentials.containsKey(userID)) {
                    dos.writeUTF("userID wrong");
                    dos.flush();
                    userIDFlag = 0;
                }
            } while (userIDFlag == 0);

            // Password validation section
            String password = "";
            int loginFlag = 0;
            do {
                password = dis.readUTF();
                // Password collect
                if (TCPServer.Credentials.get(userID).equals(password)) {
                    TCPServer.loginAttempt.put(userID, 0);
                    dos.writeUTF("password collect");
                    dos.flush();
                    System.out.println("> " + userID + " login successfully");
                    loginFlag = 1;
                } else {
                    // Password wrong
                    TCPServer.loginAttempt.put(userID, TCPServer.loginAttempt.get(userID) + 1);
                    dos.writeUTF("password wrong");
                    dos.writeInt(TCPServer.loginAttempt.get(userID));
                    dos.flush();
                    loginFlag = 0;
                    // Set sever in this thread to sleep and reset loginAttempt timer
                    if (TCPServer.loginAttempt.get(userID) == 3) {
                        Thread.sleep(10000);
                        TCPServer.loginAttempt.put(userID, 0);
                    }
                }
            } while (loginFlag == 0);

            // Get available TempID of current login user
            String availableTempID = tempID.findAvailableTempID(userID);
            if (availableTempID.equals("")) {
                // No available TempID for current user
                tempID.createNewTempID(userID);
                System.out.println("> " + userID + "'s TempID: " + tempID.findAvailableTempID(userID));
            } else {
                // There is available TempID for current user
                System.out.println("> " + userID + "'s TempID: " + availableTempID);
            }

            // Keep listening commend from client
            do {
                String command = dis.readUTF();
                // "Download_tempID" command
                if (command.equals("Download_tempID")) {
                    availableTempID = tempID.findAvailableTempID(userID);
                    if (availableTempID.equals("")) {
                        // If there is no available TempID
                        tempID.createNewTempID(userID);
                        dos.writeUTF(tempID.findAvailableTempID(userID));
                    } else {
                        // There is available TempID
                        dos.writeUTF(availableTempID);
                    }
                } else if (command.equals("logout")) {
                    System.out.println("> " + userID + " logout");
                    dis.close();
                    dos.close();
                    client.close();
                    TCPServer.alreadyLogin.put(userID, false);
                } else if (command.equals("Upload_contact_log")) {
                    System.out.println("> Received contact log from " + userID);
                    int logNum = dis.readInt();
                    HashMap<String, String[]> contact_log = new HashMap<>();
                    for (int i = 0; i < logNum; i++) {
                        String log = dis.readUTF();
                        System.out.println(log);
                        String[] dataArray = log.split(" ");
                        String[] time = new String[]{dataArray[1] + " " + dataArray[2], dataArray[3] + " " + dataArray[4]};
                        contact_log.put(dataArray[0], time);
                    }
                    System.out.println("> Contact log checking");
                    tempID.checkContactLog(contact_log);
//                    System.out.println("----hashMap");
//                    for(String key : contact_log.keySet()){
//                        System.out.println(key + " " + Arrays.toString(contact_log.get(key)));
//                    }

                }
            } while (true);
        } catch (IOException | InterruptedException e) {
            // If client server terminated by user by press control + c
            if (TCPServer.alreadyLogin.containsKey(userID) && TCPServer.alreadyLogin.get(userID)) {
                // logout current user
                TCPServer.alreadyLogin.put(userID, false);
                System.out.println("> Client program terminated by " + userID);
            }
        }
    }

}

class tempID {

    // Create new tempID for certain user
    public static void createNewTempID(String userID) throws IOException {
        String pathname = "tempIDs.txt";
        boolean emptyFlag = false;
        // Check whether the tempIDs.txt file is empty
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            String line = br.readLine();
            if (line == null) {
                emptyFlag = true;
            } else {
                emptyFlag = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // input TempID into file
        try (FileWriter wr = new FileWriter(pathname, true);
             BufferedWriter bw = new BufferedWriter(wr)) {
            // Get current_time and expire_time
            long current_time = new Date().getTime();
            long expire_time = current_time + 900000;
            String input = userID + " " + generateTempID() + " " + stampToDate(current_time) + " " + stampToDate(expire_time);
            if (emptyFlag) {
                bw.write(input);
            } else {
                // IF file is not empty, change to new line before write into file
                bw.write("\r\n" + input);
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get available tempID for certain user
    public static String findAvailableTempID(String userID) throws FileNotFoundException {
        String pathname = "tempIDs.txt";
        String userID_file = "";
        String tempID_file = "";
        long finish_time = 0;
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                // Regex to get each element in line
                String[] dataArray = line.split(" ");
                userID_file = dataArray[0];
                // Check is this line has user's info
                if (userID_file.equals(userID)) {
                    tempID_file = dataArray[1];
                    // Convert date -> timestamp
                    finish_time = dateToStamp(dataArray[4] + " " + dataArray[5]);
                }
            }
            // Compare current time with expire_time get from tempIDs.txt
            long current_time = new Date().getTime();
            if (current_time > finish_time) {
                // Not available
                return "";
            } else {
                // Available
                return tempID_file;
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void checkContactLog(HashMap<String, String[]> contact_log) throws FileNotFoundException {
        String pathname = "tempIDs.txt";
        for (String key : contact_log.keySet()) {
            try (FileReader fr = new FileReader(pathname);
                 BufferedReader br = new BufferedReader(fr)) {
                 String line;
                 while((line = br.readLine()) != null){
                     String[] dataArray = line.split(" ");
                     if(dataArray[1].equals(key)){
                         System.out.println(dataArray[0] + " " + dataArray[2] + " " + dataArray[3] + " " + dataArray[1]);
                         break;
                     }
                 }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Date -> timestamp
    public static long dateToStamp(String time) throws ParseException, ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = simpleDateFormat.parse(time);
        long ts = date.getTime();
        return ts;
    }

    // Generate tempID string combine with 3 random int + timestamp(ms) + 4 random int
    public static String generateTempID() {
        String tempID = String.valueOf(System.currentTimeMillis());
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            tempID += String.valueOf(random.nextInt(10));
        }
        for (int i = 0; i < 3; i++) {
            tempID = String.valueOf(random.nextInt(10)) + tempID;
        }
        return tempID;
    }

    // timestamp -> date
    public static String stampToDate(long stap) {
        String time;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        long lt = stap;
        Date date = new Date(lt);
        time = simpleDateFormat.format(date);
        return time;
    }


}


