import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TCPClient {

    public static boolean ThreadFlag = true;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(">>> TCP Client start ...");

        String server_ip = args[0];
        int server_port = Integer.parseInt(args[1]);
        int client_udp_port = Integer.parseInt(args[2]);
        System.out.println("> Server_ip: " + server_ip);
        System.out.println("> Server_port: " + server_port);
        System.out.println("> Client_udp_port: " + client_udp_port);
        System.out.println("----------------- TCPClient -----------------");

        Client(server_ip, server_port, client_udp_port);
    }

    public static void Client(String server_ip, int server_port, int clientPort) throws IOException, InterruptedException {
        // Create socket object
        Socket s = new Socket(InetAddress.getByName(server_ip), server_port);
        Channel(s, clientPort);
        s.close();
    }

    public static void Channel(Socket s, int clientPort) throws IOException, InterruptedException {
        // Set stream
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        DataInputStream dis = new DataInputStream(s.getInputStream());

        // Set reader
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        // Username validation section
        String userID = "";
        int userIDFlag = 0;
        do {
            System.out.print("> userID: ");
            // Get username from console input
            userID = console.readLine();
            dos.writeUTF(userID);
            dos.flush();
            String message = dis.readUTF();
            if (message.equals("userID existed")) {
                // UserID correct
                userIDFlag = 1;
            } else if (message.equals("already login")) {
                // User already login or is logining in other terminal windows
                System.out.println("> User already login or is processing login in other terminal");
                userIDFlag = 0;
            } else if (message.equals("user block")) {
                // User was during block period in other terminal
                System.out.println("> Your account has been blocked due to multiple login failures. Please try again later");
                userIDFlag = 0;
                Thread.sleep(10000);
            } else {
                // Wrong username
                System.out.println("> Invalid username. Please try again");
                userIDFlag = 0;
            }
        } while (userIDFlag == 0);

        // Password validation section
        String password = "";
        int loginFlag = 0;
        int loginAttempt = 0;
        do {
            System.out.print("> password: ");
            // Get password from console input
            password = console.readLine();
            dos.writeUTF(password);
            dos.flush();
            if (dis.readUTF().equals("password collect")) {
                // Password correct
                System.out.println("> Welcome to the BlueTrace Simulator");
                loginFlag = 1;
                loginAttempt = 0;
            } else {
                // Password wrong
                // Get available attempt times from server
                loginAttempt = dis.readInt();
                String message = "> Invalid password. You can still attempt " + (3 - loginAttempt);
                String time = (loginAttempt == 2) ? " time." : " times.";
                message = message + time;
                // If current user already try 3 times
                if (loginAttempt < 3) {
                    System.out.println(message);
                } else {
                    System.out.println("> Invalid password. Your account has been blocked. Please try again later");
                    Thread.sleep(10000);
                }
                loginFlag = 0;
            }
        } while (loginFlag == 0);

        // Finish validation, wait for next command
        // Start UDR receiver thread to keep listening from other client
        UDPRcv UDPRecever = new UDPRcv(clientPort);
        new Thread(UDPRecever).start();
        new Thread(new contactLog()).start();
        do {
            // Get next command input by user from console input
            System.out.print("> ");
            String command = console.readLine();
            // Split the command to check Beacon command
            String[] commandArray = command.split(" ");
            if (command.equals("Download_tempID")) {
                // Download_tempID
                dos.writeUTF(command);
                dos.flush();
                String response = dis.readUTF();
                System.out.println("tempID:" + "\n\r" + response);
            } else if (command.equals("logout")) {
                // logout
                dos.writeUTF(command);
                // Release resource
                dos.flush();
                dos.close();
                dis.close();
                s.close();
                // Send a UDP packet to myself to stop the UDP listening thread
                UDPsend.send("127.0.0.1", clientPort, "stop");
                TCPClient.ThreadFlag = false;
                break;
            } else if (command.equals("Upload_contact_log")) {
                // Upload_contact_log
                dos.writeUTF(command);
                dos.flush();
                contactLog.upload(dos);
            } else if (commandArray.length == 3 && commandArray[0].equals("Beacon")) {
                // Beacon
                dos.writeUTF("beacon");
                dos.flush();
                // Get UDP IP address and port from input
                String targetIP = commandArray[1];
                int targetPort = Integer.parseInt(commandArray[2]);
                // Wait beacon message send by server
                String beaconMessage = dis.readUTF();
                UDPsend.send(targetIP, targetPort, beaconMessage);
                String[] dataArray = beaconMessage.split(",");
                System.out.println(dataArray[0] + ",\r\n" + dataArray[1] + ",\r\n" + dataArray[2] + ".");
            } else {
                // Invalid command
                System.out.println("> Error. Invalid command");
            }
        } while (true);
    }
}

// Class for uploading contact log
class contactLog implements Runnable{

    public void run(){
        while(TCPClient.ThreadFlag) {
            String new_content = "";
            String pathname = "z5142012_contactlog.txt";
            try (FileReader fr = new FileReader(pathname);
                 BufferedReader br = new BufferedReader(fr)) {
                String line = "";
                while ((line = br.readLine()) != null) {
                    String[] dataArray = line.split(" ");
                    long current_time = new Date().getTime();
//                    System.out.println(dataArray[3] + " " + dataArray[4]);
//                    System.out.println(dataArray[5] + " " + dataArray[6]);
                    long received_time = UDPRcv.dateToStamp(dataArray[5] + " " + dataArray[6]);
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
            try (FileWriter wr = new FileWriter(pathname, false);
                 BufferedWriter bw = new BufferedWriter(wr)) {
                bw.write(new_content);
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            System.out.println(new_content);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void upload(DataOutputStream dos) throws IOException {
        String pathname = "z5142012_contactlog.txt";
        int lineNum = 0;
        ArrayList<String> content = new ArrayList<>();
        // Read content from local file
        try (FileReader fr = new FileReader(pathname);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] lineArray = line.split(" ");
                String tempLine = lineArray[0] + ",\r\n" + lineArray[1] + " " + lineArray[2] + ",\r\n" + lineArray[3] + " " + lineArray[4] + ";";
                String message = lineArray[0] + " " + lineArray[1] + " " + lineArray[2] + " " + lineArray[3] + " " + lineArray[4];
                content.add(message);
                System.out.println(tempLine);
                lineNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Firstly send line number of contactlog to server
        dos.writeInt(lineNum);
        dos.flush();
        // Send every line of content to server
        for (int i = 0; i < lineNum; i++) {
            dos.writeUTF(content.get(i));
            dos.flush();
        }
    }
    //TODO: 收到beacon，如果valid，写入contactlog
    public static void store(String beaconContent) throws IOException {
        String pathname = "z5142012_contactlog.txt";
        boolean emptyFlag = false;
        // Check whether the tempIDs.txt file is empty
        beaconContent = beaconContent + " " + UDPRcv.stampToDate(new Date().getTime());
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
            BufferedWriter bw = new BufferedWriter(wr)){
            if(emptyFlag){
                bw.write(beaconContent);
            } else {
                bw.write("\r\n" + beaconContent);
            }
            bw.flush();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    //TODO：multiple thread 来检测message是否过期

}

// UDP send class
class UDPsend {
    public static void send(String IPAddress, int port, String content) throws IOException {
        byte[] data;
        data = content.getBytes();
        InetAddress inetAddress = InetAddress.getByName(IPAddress);
        DatagramPacket dp = new DatagramPacket(data, data.length, inetAddress, port);
        DatagramSocket ds = new DatagramSocket();
        ds.send(dp);
        ds.close();
    }
}

// UDP receive class
class UDPRcv implements Runnable {

    int port;

    public UDPRcv(int port) {
        this.port = port;
    }

    // multiple thread
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
            // DatagramPacket -> String
            String content = new String(data, 0, dp.getLength());
            // Stop thread if receive command send by client
            if (content.equals("stop")) break;
            String[] contentArray = content.split(",");
            System.out.println("\r\nReceived beacon:");
            System.out.println(contentArray[0] + ",\r\n" + contentArray[1] + ",\r\n" + contentArray[2] + ".");
            System.out.println("Current time is:\r\n" + stampToDate(new Date().getTime()) + ".");
            // Beacon validation
            if(beaconValid(content)){
                System.out.println("The beacon is valid.");
                try {
                    contactLog.store(contentArray[0] + " " + contentArray[1] + " " + contentArray[2]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else System.out.println("The beacon is invalid.");
            System.out.print("> ");
        }
        ds.close();
    }

    // Beacon validation vheck
    public static Boolean beaconValid(String beaconMessage){
        String[] contentArray = beaconMessage.split(",");
        long current_time = new Date().getTime();
        long expire_time = 0;
        try {
            expire_time = dateToStamp(contentArray[2]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (current_time <= expire_time) return true;
        else return false;
    }

    // Date -> timestamp
    public static long dateToStamp(String time) throws ParseException, ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = simpleDateFormat.parse(time);
        long ts = date.getTime();
        return ts;
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
