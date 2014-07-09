import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**************************************************************************************************
 *
 * CS456 A1 
 * Sender function using Go-Back-N protocol
 *
 *************************************************************************************************/

//*************************************************************************************************
public class sender {
    private static Object lock = new Object();

    private final static int winSize = 10;
    private final static int packetSize = 1024;  // we actually want 512 bytes,but since one char in java is 2 bytes
    
    private static int base = 0;
    private static int nextSeqNum = 0;
    
    private static BufferedWriter seqLog;
    private static BufferedWriter ackLog;
    
    //*********************************************************************************************    
    public static void main(String[] args) throws Exception {
        try {
            if (args.length < 4) {
                String str = "Useage: \n" +
                            "java sender <arguments> \n\n" +
                            "<arguments>: \n" +
                            "\t<host addr of emulator>\n" +
                            "\t<UDP port used by emulator to receive data from the sender>\n" +
                            "\t<UDP port used by sender to receive data from the emulator\n" +
                            "\r<name of the file to be sent\n";
                throw new RuntimeException(str);
            
            }
        
            // Parse command-line arguments
            InetAddress emulatorAddr = InetAddress.getByName(args[0]);             
            int emulatorPort = Integer.parseInt(args[1]);
            int senderPort = Integer.parseInt(args[2]);
            File targetFile = new File(args[3]);          
        
            // Create log files 
            seqLog = new BufferedWriter(new FileWriter("seqnum.log"));
            ackLog = new BufferedWriter(new FileWriter("ack.log"));
        
        
            // Create a list of target packet to be sent
            List<packet> packetsToSend = cutFile(targetFile);  
            DatagramSocket senderSocket = new DatagramSocket(senderPort);
                
            // Start sending packets
            start(packetsToSend, senderSocket, emulatorAddr, emulatorPort);
        
            seqLog.close();
            ackLog.close();
            senderSocket.close();
        } catch (UnknownHostException ex) {
            System.out.println("Sender: error: " + ex.getMessage());
        }
    }
    //**********************************************************************************************
    private static List<packet> cutFile(File targetFile) {
        List<packet> chunk = new ArrayList<packet>();
        try {
            // Open a reader to the file
            BufferedReader fileBuffer = new BufferedReader(new FileReader(targetFile));
            // Divide the file into 500-char packets
            int index = 0;
            while (fileBuffer.ready()) {
                char[] fileData = new char[500];
                int len = fileBuffer.read(fileData, 0, 500);
                fileData = Arrays.copyOf(fileData, len);
            
                chunk.add(packet.createPacket(index, new String(fileData)));
                index++;
            }
        } catch (Exception ex) {
            System.out.println("Error: Error when creating a list.");
        }
        return chunk;
    }
    
    
    //**********************************************************************************************
    private static void start(final List<packet> targetFile, final DatagramSocket senderSocket, final InetAddress emulatorAddr,
                              final int emulatorPort) throws Exception {
        // Create two threads: one for sending packets, the one for receiving packets
        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendPackets(targetFile, senderSocket, emulatorAddr, emulatorPort);      // ////////
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    receivePacket(targetFile, senderSocket, emulatorAddr, emulatorPort);        // /////
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // start the two threads.  
        sendThread.start();
        receiveThread.start();
        
        // Wait for thest two threads to die. 
        sendThread.join();
        receiveThread.join();
        
        // Send the EOT packet and record it.
        packet newPacket = packet.createEOT(nextSeqNum);
        makeAndSend(newPacket, senderSocket, emulatorAddr, emulatorPort);      
        ackLog.write(String.format("%d\n", nextSeqNum % 32));
        ackLog.flush();

    }
    
    //********************************************************************************************** 
    // Continuously try to send packets as the window shifts
    private static void sendPackets(List<packet> packetList, DatagramSocket senderSocket, InetAddress emuAddr, int emuPort) throws Exception {
        
        // Keep trying to send until there are no packets left to send
        while (true) {
            synchronized (lock) {
                if (nextSeqNum < base + winSize) {
                    makeAndSend(packetList.get(nextSeqNum), senderSocket, emuAddr, emuPort);       
                    nextSeqNum++;
                }
                
                // Return if we're done sending all of the data
                if (nextSeqNum == packetList.size()) {
                    return;
                }
            }
        }
    }
    //**********************************************************************************************
    // Listen for incoming ACKs, and timeout if none received
    private static void receivePacket(List<packet> packetList, DatagramSocket senderSocket, InetAddress emuAddr, int emuPort) throws Exception {
        int timer = 100;

        while (true) {
            try {
                //DatagramPacket receivedPacket = createEmptyPacket();
                DatagramPacket receivedPacket = new DatagramPacket(new byte[packetSize],packetSize);

                // receive() blocks form "timer" amount of time on this socket, throws exception if timeouted. 
                senderSocket.setSoTimeout(timer);
                senderSocket.receive(receivedPacket);
                
                //*********************************
                // Check if ACKed seqnum is valide
                int ackedSeqNum = -1;
                synchronized (lock) {
                    packet ack = packet.parseUDPdata(receivedPacket.getData());
                    int ackSeqNum = ack.getSeqNum();
                    
                    // check if teh received ackedSeqNum is valid.
                    // base <= ackedSeqNum + offset * 32 <= base + winSize
                    // Also, we need to exclude the following case:
                    // (ackedSeqNum + offset * 32) < base and (ackedSeqNum + (offset + 1) * 32 ) > (base + winSize)
                    int offset = 0;
                    if (!(((ackSeqNum + offset * 32) < base) && ((ackSeqNum + (offset+1) * 32) > (base +winSize)))) {
                        while (((ackSeqNum + offset * 32) < base) || ((ackSeqNum + offset * 32) > (base + winSize))){
                            offset++;
                        }
                        base = (ackSeqNum + 32 * offset)+1;
                    }
                    ackLog.write(String.format("%d\n", ackSeqNum));
                    ackLog.flush();
                    ackedSeqNum = base;
                }
                //**********************************
                
                // Exit if this is the EOT packet
                if (ackedSeqNum == packetList.size()) {
                    return;
                }
                
            } catch (SocketTimeoutException e) {
                // Re-send all packets within window if no ack within timeout
                synchronized (lock) {
                    for (int i = base; i < nextSeqNum; i++) {
                        packet nextPacket = packetList.get(i);
                        makeAndSend(nextPacket, senderSocket, emuAddr, emuPort);
                    }
                }
            }
        }
        
    }

    //**********************************************************************************************    
    // Create and send a packet
    private static void makeAndSend(packet packetToSend, DatagramSocket senderSocket, InetAddress emuAddr, int emuPort) throws Exception {
        DatagramPacket transportPacket = new DatagramPacket(new byte[packetSize],packetSize);
        
        transportPacket.setAddress(emuAddr);
        transportPacket.setPort(emuPort);
        transportPacket.setData(packetToSend.getUDPdata());
        
        senderSocket.send(transportPacket);
        
        // Write the sequence number to seqLog
        seqLog.write(String.format("%d\n", packetToSend.getSeqNum()));
        seqLog.flush();
    }
    
}