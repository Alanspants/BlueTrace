import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {


    static Map<String, Integer> loginAttempt = new HashMap<String, Integer>() {};
    static Map<String, String> Credentials = new HashMap<String, String>() {};
    static Map<String, Boolean> alreadyLogin = new HashMap<String, Boolean>() {};

    public static void main(String[] args) throws IOException {
        setHashMap();

        int server_port = Integer.parseInt(args[0]);
        int block_duration = Integer.parseInt(args[1]) * 1000;
        ServerSocket ss = new ServerSocket(server_port);
        while (true) {
            Socket client = ss.accept();
            new Thread(new Channel(client, block_duration)).start();
        }
    }

    // Function to initiate three hashMap.
    public static void setHashMap() throws IOException {
        String pathname = "credentials.txt";
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            // Read file line to line.
            String line;
            while ((line = br.readLine()) != null) {

                // Regex data to get UserID and password.
                String[] dataArray = line.split(" ");
                String UserID = dataArray[0];
                String password = dataArray[1];

                // Put data into hashMap.
                Server.Credentials.put(UserID, password);
                Server.loginAttempt.put(UserID, 0);
                Server.alreadyLogin.put(UserID, false);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Channel implements Runnable {

    Socket client;

    int block_duration;

    public Channel(Socket client, int block_duration) {
        this.client = client;
        this.block_duration = block_duration;
    }

    // Multiple Thread function of socket.
    public void run() {
        String userID = "";
        try {
            // Set DataInputStream and DataOutputStream.
            DataInputStream dis = new DataInputStream(client.getInputStream());
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());

            // Send block_duration to client
            dos.writeInt(block_duration);
            dos.flush();

            // Username validation.
            userID = usernameValidation(block_duration, dis, dos);

            // Password validation.
            passwordValidation(block_duration, userID, dis, dos);

            // Get available TempID of current login user.
            String availableTempID = tempID.findAvailableTempID(userID);
            if (availableTempID.equals("")) {
                // No available TempID for current user.
                tempID.createNewTempID(userID);
                System.out.println("> " + userID + "'s TempID: " + tempID.findAvailableTempID(userID));
            } else {
                // There is available TempID for current user.
                System.out.println("> " + userID + "'s TempID: " + availableTempID);
            }

            // Keep listening for next command.
            nextCommand(client, userID, dis, dos);
        } catch (IOException | InterruptedException e) {
            // If client server terminated by user by press control + c.
            if (Server.alreadyLogin.containsKey(userID) && Server.alreadyLogin.get(userID)) {
                // logout current user.
                Server.alreadyLogin.put(userID, false);
                System.out.println("> Client program terminated by " + userID);
            }
        }
    }

    // Username validation function.
    public static String usernameValidation(int block_duration, DataInputStream dis, DataOutputStream dos) throws IOException, InterruptedException {
        String userID = "";
        int userIDFlag = 0;
        do {
            userID = dis.readUTF();
            if (Server.Credentials.containsKey(userID)) {

                // If this user is being blocked in other window.
                if (Server.loginAttempt.get(userID) == 3) {
                    dos.writeUTF("user block");
                    dos.flush();
                    // Reset the block period
                    Thread.sleep(block_duration);
                    Server.loginAttempt.put(userID, 0);
                    continue;
                }

                // If the user has already login.
                if (Server.alreadyLogin.get(userID)) {
                    dos.writeUTF("already login");
                    dos.flush();
                    userIDFlag = 0;
                } else {
                    Server.alreadyLogin.put(userID, true);
                    dos.writeUTF("userID existed");
                    dos.flush();
                    userIDFlag = 1;
                }

                // Invalid username.
            } else if (!Server.Credentials.containsKey(userID)) {
                dos.writeUTF("userID wrong");
                dos.flush();
                userIDFlag = 0;
            }

        } while (userIDFlag == 0);
        return userID;
    }

    // Password validation function.
    public static void passwordValidation(int block_duration, String userID, DataInputStream dis, DataOutputStream dos) throws IOException, InterruptedException {
        String password = "";
        int loginFlag = 0;
        do {
            password = dis.readUTF();

            // Password collect.
            if (Server.Credentials.get(userID).equals(password)) {
                Server.loginAttempt.put(userID, 0);
                dos.writeUTF("password collect");
                dos.flush();
                System.out.println("> " + userID + " login successfully");
                loginFlag = 1;
            } else {
                // Password wrong.
                Server.loginAttempt.put(userID, Server.loginAttempt.get(userID) + 1);
                dos.writeUTF("password wrong");
                dos.writeInt(Server.loginAttempt.get(userID));
                dos.flush();
                loginFlag = 0;
                // Set sever in this thread to sleep and reset loginAttempt timer.
                if (Server.loginAttempt.get(userID) == 3) {
                    Thread.sleep(block_duration);
                    Server.loginAttempt.put(userID, 0);
                }
            }

        } while (loginFlag == 0);
    }

    // Function for keep listening next command.
    public static void nextCommand(Socket client, String userID, DataInputStream dis, DataOutputStream dos) throws IOException {
        do {
            String command = dis.readUTF();

            // "Download_tempID" command.
            if (command.equals("Download_tempID")) {
                String availableTempID = tempID.findAvailableTempID(userID);
                if (availableTempID.equals("")) {
                    // If there is no available TempID.
                    tempID.createNewTempID(userID);
                    dos.writeUTF(tempID.findAvailableTempID(userID));
                } else {
                    // There is available TempID.
                    dos.writeUTF(availableTempID);
                }

                // "logout" command.
            } else if (command.equals("logout")) {
                System.out.println("> " + userID + " logout");
                dis.close();
                dos.close();
                client.close();
                Server.alreadyLogin.put(userID, false);

                // "Upload_contact_log" command.
            } else if (command.equals("Upload_contact_log")) {
                System.out.println("> Received contact log from " + userID);
                int logNum = dis.readInt();
//                HashMap<String, String[]> contact_log = new HashMap<>();
                ArrayList<String> contact_log = new ArrayList<>(){};
                for (int i = 0; i < logNum; i++) {
                    String log = dis.readUTF();
                    String[] dataArray = log.split(" ");
                    String tempLine = dataArray[0] + ", " + dataArray[1] + " " + dataArray[2] + ", " + dataArray[3] + " " + dataArray[4] + ";";
                    System.out.println(tempLine);
//                    String[] time = new String[]{dataArray[1] + " " + dataArray[2], dataArray[3] + " " + dataArray[4]};
                    contact_log.add(dataArray[0]);
                }
                System.out.println("> Contact log checking");
                System.out.println(contact_log.size());
                tempID.checkContactLog(contact_log);

                // "Beacon IPAddress port" command.
            } else if (command.equals("beacon")) {
                dos.writeUTF(tempID.getBeaconMessage(userID));
                dos.flush();
            }

        } while (true);
    }

}

class tempID {

    // Create new tempID for certain user.
    public static void createNewTempID(String userID) throws IOException {
        String pathname = "tempIDs.txt";
        boolean emptyFlag = false;
        // Check whether the tempIDs.txt file is empty.
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

        // input TempID into file.
        try (FileWriter wr = new FileWriter(pathname, true);
             BufferedWriter bw = new BufferedWriter(wr)) {
            // Get current_time and expire_time.
            long current_time = new Date().getTime();
            long expire_time = current_time + 900000;
            String input = userID + " " + generateTempID() + " " + stampToDate(current_time) + " " + stampToDate(expire_time);
            if (emptyFlag) {
                bw.write(input);
            } else {
                // IF file is not empty, change to new line before write into file.
                bw.write("\r\n" + input);
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get available tempID for certain user.
    public static String findAvailableTempID(String userID) throws FileNotFoundException {
        String pathname = "tempIDs.txt";
        String userID_file = "";
        String tempID_file = "";
        long finish_time = 0;
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {

            // Find line contain user's info.
            String line;
            while ((line = br.readLine()) != null) {
                // Regex to get each element in line.
                String[] dataArray = line.split(" ");
                userID_file = dataArray[0];
                // Check is this line has user's info.
                if (userID_file.equals(userID)) {
                    tempID_file = dataArray[1];
                    // Convert date -> timestamp.
                    finish_time = dateToStamp(dataArray[4] + " " + dataArray[5]);
                }
            }

            // Compare current time with expire_time get from tempIDs.txt.
            long current_time = new Date().getTime();
            if (current_time > finish_time) {
                // Not available.
                return "";
            } else {
                // Available.
                return tempID_file;
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Get beacon message which gonna be sent to Client socket.
    public static String getBeaconMessage(String userID) {
        String versionNum = "1";
        String pathname = "tempIDs.txt";
        String userID_file = "";
        String beaconMessage = "";
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] dataArray = line.split(" ");
                userID_file = dataArray[0];
                if (userID_file.equals(userID)) {
                    beaconMessage = dataArray[1] + "," + dataArray[2] + " " + dataArray[3] + "," + dataArray[4] + " " + dataArray[5] + " " + versionNum;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return beaconMessage;
    }

    // Function for checking contact log.
    public static void checkContactLog(ArrayList<String> contact_log) throws FileNotFoundException {
        String pathname = "tempIDs.txt";
        for (int index = 0; index < contact_log.size(); index++) {
            try (FileReader fr = new FileReader(pathname);
                 BufferedReader br = new BufferedReader(fr)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] dataArray = line.split(" ");
                    if (dataArray[1].equals(contact_log.get(index))) {
                        System.out.println(dataArray[0] + ", " + dataArray[2] + " " + dataArray[3] + ", " + dataArray[1] + ";");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Generate tempID string combine with 3 random int + timestamp(ms) + 4 random int.
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

    // timestamp -> date.
    public static String stampToDate(long stap) {
        String time;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        long lt = stap;
        Date date = new Date(lt);
        time = simpleDateFormat.format(date);
        return time;
    }

    // Date -> timestamp.
    public static long dateToStamp(String time) throws ParseException, ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = simpleDateFormat.parse(time);
        long ts = date.getTime();
        return ts;
    }
}


