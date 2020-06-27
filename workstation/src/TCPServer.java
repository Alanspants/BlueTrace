import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

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
        new Thread(new tempID()).start();
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
//                        TCPServer.alreadyLogin.put(userID, false);
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
            // Get available TempID of current login user
            String availableTempID = tempID.findAvailableTempID(userID);
            if (availableTempID.equals("")){
                tempID.createNewTempID(userID);
                System.out.println("> " + userID + "'s TempID: " + tempID.findAvailableTempID(userID));
            } else {
                System.out.println("> " + userID +"'s TempID: " + availableTempID);
            }

            do {
                String command = dis.readUTF();
                //持续从client来接受命令，如果中途control+c退出则视为退出当前account
                if (command.equals("Download_tempID")){
                    System.out.println("sdadsad");
                    dos.writeUTF(tempID.findAvailableTempID(userID));
                    dos.flush();
                }
            } while (true);
        } catch (IOException | InterruptedException e) {
            if (TCPServer.alreadyLogin.containsKey(userID) && TCPServer.alreadyLogin.get(userID)) {
                TCPServer.alreadyLogin.put(userID, false);
            }
            if (userID != "") {
                System.out.println("> Client program terminated by " + userID);
            }
        }
    }

}

class tempID implements Runnable{
    public void run(){
        int i = 0;
        while(true){
            System.out.println(i++);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void createNewTempID(String userID) throws IOException {
        String pathname = "tempIDs.txt";
        boolean emptyFlag = false;
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)){
             String line = br.readLine();
             if(line == null){
                 emptyFlag = true;
             } else {
                 emptyFlag = false;
             }
        } catch (IOException e){
            e.printStackTrace();
        }
        try(FileWriter wr = new FileWriter(pathname,true);
            BufferedWriter bw = new BufferedWriter(wr)){
            long current_time = new Date().getTime();
            long expire_time = current_time + 900000;
//            System.out.println("current_time: " + current_time);
//            System.out.println("expire_time: " + expire_time);
            String input = userID + " " + generateTempID() + " " + stampToDate(current_time) + " " + stampToDate(expire_time);
//            bw.write(input + "\r\n");
            if (emptyFlag) {
                bw.write(input);
            } else {
                bw.write("\r\n" + input);
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String findAvailableTempID(String userID) throws FileNotFoundException {
        String pathname = "tempIDs.txt";
        String userID_file = "";
        String tempID_file = "";
        long finish_time = 0;
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)){
            String line;
            while ((line = br.readLine()) != null) {
                String[]  dataArray = line.split(" ");
                userID_file = dataArray[0];
//                System.out.println(userID_file);
                if(userID_file.equals(userID)){
                    tempID_file = dataArray[1];
                    finish_time = dateToStamp(dataArray[4] + " " + dataArray[5]);
                }
            }
            long current_time = new Date().getTime();
            if (current_time > finish_time){
                return "";
            } else {
                return tempID_file;
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static long dateToStamp(String time) throws ParseException, ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = simpleDateFormat.parse(time);
        long ts = date.getTime();//获取时间的时间戳
        return ts;
    }

    public static String generateTempID(){
        String tempID = String.valueOf(System.currentTimeMillis());
        Random random = new Random();
        for (int i = 0; i < 4; i++){
            tempID += String.valueOf(random.nextInt(10));
        }
        for (int i = 0; i < 3; i++){
            tempID = String.valueOf(random.nextInt(10)) + tempID;
        }
        return tempID;
    }

    public static String stampToDate(long stap){
        String time;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        long lt = stap;
        Date date = new Date(lt);
        time = simpleDateFormat.format(date);
        return time;
    }


}

