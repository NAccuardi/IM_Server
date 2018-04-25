/**
 * Created by Robot Laptop on 4/24/2018.
 */

import javax.swing.JFrame;

public class ServerTest {
    public static void main(String arg[]){
        Server testingServer = new Server();
        testingServer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testingServer.turnOnAndRunServer();
    }

}
