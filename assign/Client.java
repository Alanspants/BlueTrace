import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    // Flag to control thread run process.
    public static boolean ThreadFlag = true;

    static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws IOException, InterruptedException {
        String server_ip = args[0];
        int server_port = Integer.parseInt(args[1]);
        int client_udp_port = Integer.parseInt(args[2]);

        Socket s = new Socket(InetAddress.getByName(server_ip), server_port);
        Channel(s, client_udp_port);
        s.close();
    }

    public static void Channel(Socket s, int clientPort) throws IOException, InterruptedException {
        // Set stream.
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        DataInputStream dis = new DataInputStream(s.getInputStream());

        // Set reader.
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        // Get block_duration from server
        int block_duration = dis.readInt();

        // Username validation.
        String userID = usernameValidation(block_duration, dos, dis, console);

        // Password validation.
        passwordValidation(block_duration, dos, dis, console);

        // Finish login validation, wait for next command.
        nextCommand(clientPort, s, dos, dis, console);
    }

    // Username validation function.
    public static String usernameValidation(int block_duration, DataOutputStream dos, DataInputStream dis, BufferedReader console) throws IOException, InterruptedException {
        String userID = "";
        int userIDFlag = 0;
        do {
            System.out.print("> userID: ");
            // Get username from console input.
            userID = console.readLine();
            dos.writeUTF(userID);
            dos.flush();
            String message = dis.readUTF();
            if (message.equals("username_validation: userID existed")) {
                // UserID correct.
                userIDFlag = 1;
            } else if (message.equals("username_validation: already login")) {
                // User already login or is logining in other terminal windows.
                System.out.println("> User already login or is processing login in other terminal");
                userIDFlag = 0;
            } else if (message.equals("username_validation: user block")) {
                // User was during block period in other terminal.
                System.out.println("> Your account has been blocked due to multiple login failures. Please try again later");
                userIDFlag = 0;
                Thread.sleep(block_duration);
            } else {
                // Wrong username.
                System.out.println("> Invalid username. Please try again");
                userIDFlag = 0;
            }
        } while (userIDFlag == 0);
        return userID;
    }

    // Password validation function
    public static void passwordValidation(int block_duration, DataOutputStream dos, DataInputStream dis, BufferedReader console) throws IOException, InterruptedException {
        String password = "";
        int loginFlag = 0;
        int loginAttempt = 0;
        do {
            System.out.print("> password: ");
            // Get password from console input.
            password = console.readLine();
            dos.writeUTF(password);
            dos.flush();
            if (dis.readUTF().equals("password_validation: password collect")) {
                // Password correct.
                System.out.println("> Welcome to the BlueTrace Simulator");
                loginFlag = 1;
                loginAttempt = 0;
            } else {
                // Password wrong.
                // Get available attempt times from server.
                loginAttempt = dis.readInt();
                String message = "> Invalid password. You can still attempt " + (3 - loginAttempt);
                String time = (loginAttempt == 2) ? " time." : " times.";
                message = message + time;
                // If current user already try 3 times.
                if (loginAttempt < 3) {
                    System.out.println(message);
                } else {
                    System.out.println("> Invalid password. Your account has been blocked. Please try again later");
                    Thread.sleep(block_duration);
                }
                loginFlag = 0;
            }
        } while (loginFlag == 0);
    }

    // Listen to next command input by user.
    public static void nextCommand(int clientPort, Socket s, DataOutputStream dos, DataInputStream dis, BufferedReader console) throws IOException {
        // Thread for auto updating tempID.
        new Thread(new tempIDUpdate(new Date().getTime(), dos, dis)).start();
        // Thread for keep listening beacon packet.
        new Thread(new UDPRcv(clientPort)).start();
        // Thread for keep deleting expire contact log record.
        new Thread(new contactLog()).start();
        do {
            // Get next command input by user from console input.
            System.out.print("> ");
            String command = console.readLine();
            // Split the command to check Beacon command.
            String[] commandArray = command.split(" ");

            if (command.equals("Download_tempID")) {
                // Download_tempID.
                dos.writeUTF("command: Download_tempID");
                dos.flush();
                String response = dis.readUTF();
                System.out.println("> tempID: " + response);
            } else if (command.equals("logout")) {
                // logout.
                dos.writeUTF("command: logout");
                // Release resource.
                dos.flush();
                dos.close();
                dis.close();
                s.close();
                // Send a UDP packet to myself to stop the UDP listening thread.
                UDPSend.send("127.0.0.1", clientPort, "stop");
                Client.ThreadFlag = false;
                break;
            } else if (command.equals("Upload_contact_log")) {
                // Upload_contact_log.
                dos.writeUTF("command: Upload_contact_log");
                dos.flush();
                contactLog.upload(dos);
            } else if (commandArray.length == 3 && commandArray[0].equals("Beacon")) {
                // Beacon.
                dos.writeUTF("command: beacon");
                dos.flush();
                // Get UDP IP address and port from input.
                String targetIP = commandArray[1];
                int i = 0, flag = 0;
                // Check IP address format.
                for (i = 0; i < targetIP.length(); i++) {
                    char word = targetIP.charAt(i);
                    if (word == '.') flag++;
                    if (!(Character.isDigit(word) || word == '.')) {
                        System.out.println("> Usage: Beacon <dest IP> <dest port>");
                        break;
                    }
                }
                if (i != targetIP.length()) continue;
                if (flag != 3){
                    System.out.println("> Usage: Beacon <dest IP> <dest port>");
                    continue;
                }
                int targetPort = Integer.parseInt(commandArray[2]);
                // Wait beacon message send by server.
                String beaconMessage = dis.readUTF();
                try {
                    UDPSend.send(targetIP, targetPort, beaconMessage);
                } catch (IOException|IllegalArgumentException e) {
                    // Beacon send failed.
                    System.out.println("> Beacon message send failed, please try again.\r\n> Usage: Beacon <dest IP> <dest port>");
                    continue;
                }
                // Print information to console.
                String[] dataArray = beaconMessage.split(",");
                System.out.println(dataArray[0] + ", " + dataArray[1] + ", " + dataArray[2] + ".");
            } else {
                // Invalid command.
                System.out.println("> Error. Invalid command");
            }
        } while (true);
    }

    // Date -> timestamp.
    public static long dateToStamp(String time) throws ParseException, ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = simpleDateFormat.parse(time);
        long ts = date.getTime();
        return ts;
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
}

// Class for uploading contact log.
class contactLog implements Runnable {

    public static Lock lock = new ReentrantLock();

    // Multiple Thread for keep deleting expire record.
    public void run() {
        while (Client.ThreadFlag) {
            lock.lock();
            String new_content = "";
            String pathname = "z5142012_contactlog.txt";
            // Put all unexpired record in a String.
            try (FileReader fr = new FileReader(pathname);
                 BufferedReader br = new BufferedReader(fr)) {
                String line = "";
                while ((line = br.readLine()) != null) {
                    String[] dataArray = line.split(" ");
                    long current_time = new Date().getTime();
                    long received_time = Client.dateToStamp(dataArray[5] + " " + dataArray[6]);
                    if (received_time + 180000 > current_time) {
                        if (new_content.equals("")) {
                            new_content += line;
                        } else {
                            new_content += "\r\n" + line;
                        }
                    }
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

            // Replace file content to String generated above.
            try (FileWriter wr = new FileWriter(pathname, false);
                 BufferedWriter bw = new BufferedWriter(wr)) {
                bw.write(new_content);
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lock.unlock();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    // Upload contactlog.txt to server.
    public static void upload(DataOutputStream dos) throws IOException {
        String pathname = "z5142012_contactlog.txt";
        int lineNum = 0;
        ArrayList<String> content = new ArrayList<>();
        // Read content from local file.
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] lineArray = line.split(" ");
                String tempLine = lineArray[0] + ", " + lineArray[1] + " " + lineArray[2] + ", " + lineArray[3] + " " + lineArray[4] + ";";
                String message = lineArray[0] + " " + lineArray[1] + " " + lineArray[2] + " " + lineArray[3] + " " + lineArray[4];
                content.add(message);
                System.out.println(tempLine);
                lineNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Firstly send line number of contactlog to server.
        dos.writeInt(lineNum);
        dos.flush();
        // Send every line of content to server.
        for (int i = 0; i < lineNum; i++) {
            dos.writeUTF(content.get(i));
            dos.flush();
        }
    }

    // Store received beacon to contactlog.txt.
    public static void store(String beaconContent) throws IOException, InterruptedException {
        lock.lock();
        String pathname = "z5142012_contactlog.txt";
        boolean emptyFlag = false;
        // Check whether the tempIDs.txt file is empty.
        beaconContent = beaconContent + " " + Client.stampToDate(new Date().getTime());
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
        try (FileWriter wr = new FileWriter(pathname, true);
             BufferedWriter bw = new BufferedWriter(wr)) {
            if (emptyFlag) {
                bw.write(beaconContent);
            } else {
                bw.write("\r\n" + beaconContent);
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lock.unlock();
    }
}

// UDP send.
class UDPSend {
    public static void send(String IPAddress, int port, String content) throws IOException {
        byte[] data;
        // Adding version number to UDP message
        String versionNumber = "1";
        content += "," + versionNumber;
        data = content.getBytes();
        InetAddress inetAddress = InetAddress.getByName(IPAddress);
        DatagramPacket dp = new DatagramPacket(data, data.length, inetAddress, port);
        DatagramSocket ds = new DatagramSocket();
        ds.send(dp);
        ds.close();
    }
}

// UDP receive.
class UDPRcv implements Runnable {

    int port;

    public UDPRcv(int port) {
        this.port = port;
    }

    // Multiple Thread for keeping listening beacon packet.
    public void run() {
        DatagramSocket ds = null;
        while (true) {
            try {
                ds = new DatagramSocket(this.port);
            } catch (SocketException e) {
                continue;
            }
            byte[] data = new byte[1024];
            DatagramPacket dp = new DatagramPacket(data, data.length);
            try {
                ds.receive(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // DatagramPacket -> String.
            String content = new String(data, 0, dp.getLength());

            // Stop thread if receive command send by client.
            String[] contentArray = content.split(",");
            if(contentArray[0].equals("stop")) break;

            System.out.print("\r\nReceived beacon: ");
            System.out.println(contentArray[0] + ", " + contentArray[1] + ", " + contentArray[2] + ".");
            System.out.println("Current time is: " + Client.stampToDate(new Date().getTime()) + ".");

            // Beacon validation.
            if (beaconValid(content)) {
                System.out.println("The beacon is valid.");
                try {
                    contactLog.store(contentArray[0] + " " + contentArray[1] + " " + contentArray[2]);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else System.out.println("The beacon is invalid.");
            System.out.print("> ");
        }
        ds.close();
    }

    // Beacon validation check.
    public static Boolean beaconValid(String beaconMessage) {
        String[] contentArray = beaconMessage.split(",");
        long current_time = new Date().getTime();
        long expire_time = 0;
        try {
            expire_time = Client.dateToStamp(contentArray[2]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (current_time <= expire_time) return true;
        else return false;
    }
}

// Auto updating tempID
class tempIDUpdate implements Runnable {

    long startTime = 0;
    DataOutputStream dos;
    DataInputStream dis;

    public tempIDUpdate(long startTime, DataOutputStream dos, DataInputStream dis) {
        this.startTime = startTime;
        this.dos = dos;
        this.dis = dis;
    }

    public void run() {
        while (Client.ThreadFlag) {
            long current_time = new Date().getTime();
            // If user already login for 15 mins.
            if (current_time >= startTime + 900000) {
                try {
                    // Run Download_tempID commend to get new tempID for user.
                    dos.writeUTF("command: Download_tempID");
                    dos.flush();
                    String response = dis.readUTF();
                    System.out.println("Your have already login for 15 minutes, your new tempID is: " + response);
                    System.out.print("> ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Reset start time.
                this.startTime = current_time;
            }
            try {
                // Check every second.
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

