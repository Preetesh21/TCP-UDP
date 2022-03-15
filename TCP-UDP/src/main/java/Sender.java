import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Sender implements TCP_Connection{
    InetAddress receiver_IPAddress;
    int _MSS, dataSize, prevAck;

    List<Integer> packets;
    List<Long> timersOfPackets;
    DatagramSocket senderSocket;

    String receiver;
    int portNo;
    int lossFlag ;

    long time_out ;
    byte[] data;

    public Sender(String receive, int portN, int lossFla) throws Exception {
        portNo = portN;
        time_out = 1000;
        lossFlag = lossFla;
        receiver = receive;
        packets = new ArrayList<>();
        timersOfPackets = new ArrayList<>();
        senderSocket = new DatagramSocket();
        receiver_IPAddress = InetAddress.getByName(receiver);
        prevAck = 0;	//Initially, seqNo = 0
        _MSS = 1000;	//Packet can have a max of 1000 bytes
        dataSize = 100000;	//Total data to transmit is 100000 bytes
    }
    @Override
    public void sendPacket(int seqNo) {
        if (seqNo >= dataSize) { // Don't send. All the data has been
            // already sent.
            return;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream daos = new DataOutputStream(baos);
            daos.writeInt(seqNo);

            byte[] buff = new byte[_MSS];
            System.out.println("Adding SeqNo to packet and Sending: " + seqNo);
            System.arraycopy(data, seqNo, buff, 0, _MSS);
            daos.write(buff);
            byte[] buff2 = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(buff2, buff2.length, receiver_IPAddress, portNo);

            if (lossFlag == 0) { // Send Packet
                senderSocket.send(sendPacket);
            } else if (lossFlag == 1) {
                if (Math.random() >= 0.1) { // Send Packet
                    senderSocket.send(sendPacket);
                }
                else { // Drop Packet
                    System.out.println("Packet with seqNo: " + seqNo + " dropped.");
                }
            }
            long packetTime = (new Date()).getTime();
            timersOfPackets.add(packetTime); // Start timer for packet
            packets.add(seqNo); // Add packet to packet list
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receivePacket(DatagramSocket receiverSocket, DatagramPacket receivePacket) {
    try {
        senderSocket.setSoTimeout(100);
        senderSocket.receive(receivePacket);

        ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData());
        DataInputStream dais = new DataInputStream(bais);
        prevAck = dais.readInt();
        System.out.println("Received ACK with Acknowledgement No.:" + prevAck);

        for (int i = 0; i < packets.size(); i++) { // Clear the lists
            if (packets.get(i) < prevAck) {
                timersOfPackets.remove(i);
                packets.remove(i);
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean timerExpired() {
        long currentTime = (new Date()).getTime();
        for (int i = 0; i < packets.size(); i++) {
            long timeDiff = Math.abs(timersOfPackets.get(i) - currentTime);
            if (timeDiff > time_out) {
                System.out.println("Timer expired for seqNo " + packets.get(i));
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        String receiver = "localhost";
        if (args.length > 0) {
            receiver = (args[0]);
        }
        int portNo=1222;
        if (args.length > 1) {
            portNo = Integer.parseInt(args[1]);
        }
        int lossFlag=0;
        if (args.length > 2) {
            lossFlag = Integer.parseInt(args[2]);
        }

        Sender sender = new Sender(receiver, portNo, lossFlag);

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < sender.dataSize; i++) {
            str.append("a");
        }

        sender.data = str.toString().getBytes();

        int windowSize = 1000; // Window Size
        sender.sendPacket(sender.prevAck); // Send 1st packet

        byte[] receiveData = new byte[1300];
        while (sender.prevAck < (sender.dataSize - sender._MSS)) { 
            // For last packet, seqNo + MSS > dataSize

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            System.out.println("Waiting for ACK");

            sender.receivePacket(null, receivePacket);

            if (!sender.timerExpired()) {
                windowSize += (sender._MSS * sender._MSS / windowSize); // increase
                // window size by (MSS^2)/Window_size

                for (int i = sender.prevAck; (i + sender._MSS - sender.prevAck) < windowSize;) {
                    if (!sender.packets.contains(i)) { 
                        // if possible with the new window size, send another packet
                        sender.sendPacket(i);
                    }
                    i += sender._MSS;
                }
            } else {
                windowSize = sender._MSS; // TCP Reno - start from 1
                sender.timersOfPackets.clear(); // Clear the lists
                sender.packets.clear();
                sender.sendPacket(sender.prevAck);
            }
            System.out.println("New window size is " + windowSize);
        }
        sender.senderSocket.close();
    }
}
