
/**
 * Created by Robot Laptop on 4/24/2018.
 */

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class Server extends JFrame {
    //Variable Declaration
    private JTextField userText;//Where user can type
    private JTextArea ChatWindow;//where history will appear.
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ServerSocket server;
    private Socket connection;
    private String name;
    private String clientName;

    //Server Constructor
    public Server() {
        super("SuperAwesomeNetworkProject");
        name = JOptionPane.showInputDialog("Enter your screen name: ");
        userText = new JTextField();
        userText.setEditable(false);
        userText.addActionListener
                (
                        new ActionListener() {
                            public void actionPerformed(ActionEvent event) {
                                sendMessage(event.getActionCommand());
                                userText.setText("");
                            }
                        }
                );//end of addActionListiner
        add(userText, BorderLayout.NORTH);
        ChatWindow = new JTextArea();
        add(new JScrollPane(ChatWindow));
        setSize(300, 150);
        setVisible(true);
        ChatWindow.setEditable(false);
    }//End of COnstructor

    public void turnOnAndRunServer(){
        try{
            server = new ServerSocket(6789,20);// where the message is going, how many people wait to join.
            while (true){//this needs to be running for our server to constanlty do things.
                //bulk of server program goes here.
                try{
                    waitForSomeoneToConnect();
                    setupInputAndOutputStreamsBetweenComputers();
                    whileConnectedDoChat();
                }catch (EOFException eofException){//signals end of connection. When a user closes the program this will be displayed
                    showMessage("\n Server ended the connection.");
                }finally {
                    closeProgramDown();
                }
            }
        }catch (IOException ioException){
            ioException.printStackTrace();
        }//end trycatch
    }//turnOnServer()


    //Waits for a connection to happen, then lets you know the IP of who you connected too.
    private void waitForSomeoneToConnect() throws IOException{
        showMessage("Waiting for someone to join you. \n");
        connection = server.accept();//keeps looking for someone to connect, when they o we want to store it.
        showMessage("Now connected to "+connection.getInetAddress().getHostName());//shows the IPAddress of who you connected to.
        System.out.println(connection.getInetAddress().getHostName());
    }

    private void setupInputAndOutputStreamsBetweenComputers()throws IOException{
        output = new ObjectOutputStream(connection.getOutputStream());//lets you send things to the other person
        output.flush();//makes sure the output stream is clear after sending.
        input = new ObjectInputStream(connection.getInputStream());//Allows you to recieve messeges.
        showMessage("\n Streams are now setup. You can begin your conversation now.");
    }

    //during converation this will be what is running.
    private void whileConnectedDoChat()throws IOException{
        String payload = "You are now connected";
        sendMessage(payload);
        ableToType(true);

        do{//this is where the magic happens
            try{
                payload = (String)input.readObject();//force the
                showMessage("\n" + payload);//Display Message on a new line
            }catch (ClassNotFoundException classNotFoundException){
                showMessage("\n Stop trying to break this with odd characters.");
            }
        }while (!payload.equals("CLIENT - END"));
    }

    private  void closeProgramDown(){//close streams and clean things up.
        showMessage("\n Closing Connections Down...");
        ableToType(false); //ability to type is turned off again.
        try{
            output.close();//close output stream
            input.close();//close input stream
            connection.close();//close the connection between computers
        }catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    private void sendMessage(String payload){//sends message to the client
        //Add a second paremeter to say who sent the payload.
        try{
            output.writeObject(name + " - " + payload);
            output.flush();
            showMessage("\n"+name+" -"+payload);
        }catch(IOException ioException){
            ChatWindow.append("\n ERROR: MESSAGE UNABLE TO BE SENT");
        }
    }

    //updates ONLY the chatWindow
    private void showMessage(final String payload){
        //Change the text that appears in the chat winoow. We only want to update the window
        SwingUtilities.invokeLater(//uses a thread to add a single line of code the end of the chatwindow
                () -> ChatWindow.append(payload)
        );
    }
    //prevent typing if there is no connection.
    private void ableToType(final boolean bool){
        SwingUtilities.invokeLater(//uses a thread to add a single line of code the end of the chatwindow
                () -> userText.setEditable(bool)
        );
    }

}//end of class



