import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Timer;

/*******************************************************************************************
 *
 * CS456 A1 
 * Receiver
 *
 ******************************************************************************************/

//*******************************************************************************************
public class receiver {
    
    private static final int packetSize = 1024;
    private static BufferedWriter arrivalLog;
  
     
    public static void main(String[] args) throws Exception {
            if (args.length < 4) {
                String str = "Useage: \n" +
                        "java receiver <arguments> \n\n" +
                        "<arguments>: \n" +
                        "\t<host addr of emulator>\n" +
                        "\t<UDP port used by emulator to receive data from the receiver>\n" +
                        "\t<UDP port used by receiver to receive data from the emulator\n" +
                        "\r<name of the file to store the received file\n";
                throw new RuntimeException(str);
            }
            
            // Parse comman-line arguments
            InetAddress emulatorAddr = InetAddress.getByName(args[0]);             
            int emulatorPort = Integer.parseInt(args[1]);
            int receiverPort = Integer.parseInt(args[2]);
            File destFile = new File(args[3]);
        
            // Create log output streams
            arrivalLog = new BufferedWriter(new FileWriter("arrival.log"));

            // Create the file output stream to write to and the DatagramSocket
            FileOutputStream fileStream = new FileOutputStream(destFile);           
            DatagramSocket receiverSocket = new DatagramSocket(receiverPort);
        
            // Start reveiving packets
            start(fileStream, receiverSocket, emulatorAddr, emulatorPort);
        
            receiverSocket.close();
            arrivalLog.close();
    }
    
    //*******************************************************************************************    
    // For receiving packets
    private static void start(FileOutputStream fileStream, DatagramSocket receiverSocket, InetAddress networkHost, int networkPort)
    throws Exception {
        int lastReceivedSeqNum = -1;
        int expectedSeqNum = 0;
        for ( ; ; ) {
            DatagramPacket rcvedPacket = new DatagramPacket(new byte[packetSize], packetSize);
            receiverSocket.receive(rcvedPacket);
            packet receivedPacket = packet.parseUDPdata(rcvedPacket.getData());
            
            // Record arrival packets
            arrivalLog.write(String.format("%d\n", receivedPacket.getSeqNum()));
            
            // If packet is expected, send an ack for it
            if (receivedPacket.getSeqNum() == expectedSeqNum) {
                
                // Send an ack
                packet newPacket = packet.createACK(expectedSeqNum);
                sendPacket(newPacket, receiverSocket, networkHost, networkPort);
                lastReceivedSeqNum = expectedSeqNum;
                expectedSeqNum = (expectedSeqNum + 1) % 32;
                                 
                // If this is an EOT packet, send an EOT packet and exit. 
                if (receivedPacket.getType() == 2) {
                    sendPacket(packet.createEOT(receivedPacket.getSeqNum()), receiverSocket, networkHost, networkPort);
                    break;
                }
                
                // If packet is not EOT, then write data to the file
                fileStream.write(receivedPacket.getData());
            } else {
                // If we do not get the expected packet, re-send ACK for previous received packet
                if (lastReceivedSeqNum != -1) {
                    packet newPacket = packet.createACK(lastReceivedSeqNum);
                    sendPacket(newPacket, receiverSocket, networkHost, networkPort);
                }
                
            }
            
        }
    }
    //*******************************************************************************************
    // Create and send a new datagram packet
    private static void sendPacket(packet packetToSend, DatagramSocket socket, InetAddress emuAddr, int emuPort) throws Exception {
        byte[] sPacket = new byte[packetSize];
        DatagramPacket sendPacket = new DatagramPacket(sPacket, packetSize);   
        
        sendPacket.setAddress(emuAddr);
        sendPacket.setPort(emuPort);
        sendPacket.setData(packetToSend.getUDPdata());
        
        socket.send(sendPacket);
    }
    
}

