public interface TCP_Connection {
    void sendPacket();
    void receivePacket();
    boolean timerExpired();

}
