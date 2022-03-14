import java.net.DatagramPacket;
import java.net.DatagramSocket;

public interface TCP_Connection {
    void sendPacket( int seqNo);
    void receivePacket(DatagramSocket receiverSocket, DatagramPacket receivePacket);
    boolean timerExpired();

}
