import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

public class Receiver implements TCP_Connection{

    int portNo ;
    InetAddress address ;
    DatagramSocket receiverSocket;
    DatagramPacket receivePacket;

    int prevAck ; // cumulative ack
    List<Integer> packets = new ArrayList<>(); // packets received
    // list
    byte[] recBuff = new byte[1300];
    byte[] sendBuff = new byte[1300];

    public Receiver () throws UnknownHostException, SocketException {
        portNo = 1222;
        address = InetAddress.getByName("localhost");
        prevAck = 0;
        receiverSocket = new DatagramSocket(portNo);
        receivePacket = new DatagramPacket(recBuff, recBuff.length);
    }

    @Override
    public void sendPacket(int seqNo) {
        try {
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            System.out.println("From: " + IPAddress + " :" + port);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream daos = new DataOutputStream(baos);
            daos.writeInt(prevAck);
            sendBuff = baos.toByteArray();
            System.out.println("Send ACK with Acknowledgement No: " + prevAck);
            DatagramPacket sendPacket = new DatagramPacket(sendBuff, sendBuff.length, IPAddress, port);
            receiverSocket.send(sendPacket);
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    @Override
    public void receivePacket(DatagramSocket receiverSocket, DatagramPacket receivePacket) {
        try {
            while (true) {// Server remains always on
                System.out.println("Waiting for Packet from Sender");
                receiverSocket.receive(receivePacket);

                ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData());
                DataInputStream dais = new DataInputStream(bais);
                int seqNo = dais.readInt();
                packets.add(seqNo); // Add sequence number to list
                int packetSize = receivePacket.getLength();
                System.out.println("Received Packet size is " + packetSize);
                System.out.println("Sequence number is " + seqNo);

                Collections.sort(packets);
                boolean flag = false;
                int i = 0;
                while (i < 100000) {
                    if (!packets.contains(i)) { // Packet Missing?
                        flag = true;
                        break;
                    }
                    i += 1000;
                }
                if (flag) {
                    prevAck = i; // send prevAck
                }
                sendPacket(0);

            }
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public boolean timerExpired() {
        return false;
    }

    public static void main(String[] args) throws UnknownHostException, SocketException {
        Receiver receiver = new Receiver();
        receiver.receivePacket(receiver.receiverSocket,receiver.receivePacket);

    }
}
