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
    int _MSS, dataSize;

    InetAddress address ;
    DatagramSocket receiverSocket;
    DatagramPacket receivePacket;

    int prevAck ; // cumulative ack
    List<Integer> packets = new ArrayList<>(); // packets received
    // list
    byte[] recBuff = new byte[1300];
    byte[] sendBuff = new byte[1300];

    public Receiver (int portNo_) throws UnknownHostException, SocketException {
        portNo = portNo_;
        address = InetAddress.getByName("localhost");
        prevAck = 0;
        receiverSocket = new DatagramSocket(portNo);
        receivePacket = new DatagramPacket(recBuff, recBuff.length);
        _MSS = 1000;	//Packet can have a max of 1000 bytes
        dataSize = 100000;	//Total data to transmit is 100000 bytes
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
                while (i <= 100000) {
                    if (!packets.contains(i)) { // Packet Missing?
//                        System.out.println(i);
//                        System.out.println(packets.contains(99000));
                        flag = true;
                        break;
                    }
                    i += 1000;
                }
                if (flag) {
                    prevAck = i; // send prevAck
                }
//                System.out.println(packets.contains(seqNo));
//                System.out.println("--------------------------------------------- "+prevAck);
                sendPacket(0);
                if(prevAck > (dataSize -_MSS)){
                    packets.clear();
                    prevAck=0;
                }
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
        int portNo=1222;
        if (args.length > 0) {
            portNo = Integer.parseInt(args[1]);
        }
        Receiver receiver = new Receiver(portNo);
        receiver.receivePacket(receiver.receiverSocket,receiver.receivePacket);

    }
}
