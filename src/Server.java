
/**
 * Created by Robot Laptop on 4/24/2018.
 */

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

public class Server extends JFrame {
    //Variable Declaration
    private JTextField userText;//Where user can type
    private JTextPane ChatWindow;//where history will appear.

    private JButton imageButton;
    private String imagePath;

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ServerSocket server;
    private Socket connection;
    private String name;
    private String clientName;

    /////////private BufferedImage

    private PublicKey myPublicKey;
    private PrivateKey myPrivateKey;
    private Encryptor myEncryptor = new Encryptor();

    private PublicKey clientKey;

    //Server Constructor
    public Server() {
        super("SuperAwesomeNetworkProject");

        myPublicKey = myEncryptor.getPublicKey();
        myPrivateKey = myEncryptor.getPrivateKey();

        name = JOptionPane.showInputDialog("Enter your screen name: ");

        //place user input text box at bottom of screen
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
        add(userText, BorderLayout.SOUTH);

        //place chat box that at the top of the screen
        ChatWindow = new JTextPane();
        add(new JScrollPane(ChatWindow));
        setSize(300, 150);
        setVisible(true);
        ChatWindow.setEditable(false);

        //place add-image button
        imageButton = new JButton();
        setVisible(true);
        imageButton.setEnabled(false);
        imageButton.addActionListener
                (
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            openImageDialog();

                            BufferedImage img;
                            try {
                                img = ImageIO.read(new File(imagePath));
                            } catch (IOException ioe) {
                                return;
                            }

                            ImageIcon icon = new ImageIcon(img);
//                            ChatWindow.insertIcon(icon);
                            sendIcon(icon);
                            


                        }
                    }
                );
        add(imageButton, BorderLayout.EAST);

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
        exchangeKeys();
        showMessage("\n Streams are now setup. You can begin your conversation now.");
    }

    //during converation this will be what is running.
    private void whileConnectedDoChat()throws IOException{
        String payload = "You are now connected";
        sendMessage(payload);
        ableToType(true);


        do{//this is where the magic happens
            try{
                payload = myEncryptor.getDecryptedMessage((byte[])input.readObject());//force the
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
        //Add a second parameter to say who sent the payload.
        try{
            output.writeObject(myEncryptor.encryptString(name + " - " + payload, clientKey));
            output.flush();
            showMessage("\n"+name+" -"+payload);
        }catch(IOException ioException){
            appendString("\n ERROR: MESSAGE UNABLE TO BE SENT");
        }
    }

    //updates ONLY the chatWindow
    private void showMessage(final String payload){
        //Change the text that appears in the chat winoow. We only want to update the window
        SwingUtilities.invokeLater(//uses a thread to add a single line of code the end of the chatwindow
                () -> appendString(payload)
        );
    }

    private void sendIcon(ImageIcon icon) {
        try {
            showMessage("\n"+name+" ");
            showIcon(icon);
        } catch (Exception e){
            appendString("\n ERROR: IMAGE UNABLE TO BE SENT");
        }
    }

    private void showIcon(final ImageIcon icon) {
        SwingUtilities.invokeLater(
                () -> ChatWindow.insertIcon(icon)
        );

    }

    //prevent typing if there is no connection.
    private void ableToType(final boolean bool){
        SwingUtilities.invokeLater(//uses a thread to add a single line of code the end of the chatwindow
                () -> setUIEnabled(bool)
        );
    }

    private void setUIEnabled(boolean enabled) {
        imageButton.setEnabled(enabled);
        userText.setEditable(enabled);
    }

    private void exchangeKeys() throws IOException{
        Object o;
        try {
            o = input.readObject();
        }
        catch (ClassNotFoundException e) {
            return;
        }
        if (o instanceof PublicKey) {
            clientKey = (PublicKey)o;
        }
        output.writeObject(myPublicKey);
    }

    private void appendString(String str) {
        StyledDocument doc = (StyledDocument) ChatWindow.getDocument();
        try {
            doc.insertString(doc.getLength(), str, null);
        } catch (BadLocationException e) {
            // uh oh.
        }
    }

    private void openImageDialog() {
        JFrame frame = new JFrame();
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JPG, JPEG, & PNG images", "jpg", "jpeg", "png");
        chooser.setFileFilter(filter);

        int returnVal = chooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("You chose to open this file: " + chooser.getSelectedFile().getName());

            imagePath = chooser.getSelectedFile().getPath();

        } else if (returnVal == JFileChooser.CANCEL_OPTION) {
            return;
        }
    }

}//end of class



