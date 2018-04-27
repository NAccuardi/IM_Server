import java.io.*;
import java.net.*;
import java.awt.*;
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
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ServerSocket server;
    private Socket connection;
    private String name;

    private Encryptor myEncryptor = new Encryptor();

    private PublicKey clientKey;
    private String clientName = "Client";

    //Server Constructor
    public Server() {
        super("SuperAwesomeNetworkProject");

        name = JOptionPane.showInputDialog("Enter your screen name: ");

        //place user input text box at bottom of screen
        userText = new JTextField();
        userText.setEditable(false);
        userText.addActionListener(
                event -> {
                    sendMessage(event.getActionCommand());
                    userText.setText("");
                });//end of addActionListener
        add(userText, BorderLayout.SOUTH);

        //place chat box that at the top of the screen
        ChatWindow = new JTextPane();
        add(new JScrollPane(ChatWindow));
        setSize(500, 500);
        setVisible(true);
        ChatWindow.setEditable(false);

        //place add-image button
        imageButton = new JButton();
        setVisible(true);
        imageButton.setText("SEND IMAGE");
        imageButton.setEnabled(false);
        imageButton.addActionListener(
                e -> {
                    String imagePath = openImageDialogAndReturnPath();
                    try {
                        sendImage(new ImageIcon(ImageIO.read(new File(imagePath))));
                    } catch (IOException ioe) {
                        System.out.println("There was an IO Exception: " + ioe.getStackTrace());
                    }
                });
        add(imageButton, BorderLayout.EAST);

    }//End of Constructor

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
                    showMessage("\n i an RightServer ended the connection.");
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
        showMessage("Now connected to " + connection.getInetAddress().getHostName());//shows the IPAddress of who you connected to.
        System.out.println(connection.getInetAddress().getHostName());
    }

    private void setupInputAndOutputStreamsBetweenComputers()throws IOException{
        output = new ObjectOutputStream(connection.getOutputStream());//lets you send things to the other person
        output.flush();//makes sure the output stream is clear after sending.
        input = new ObjectInputStream(connection.getInputStream());//Allows you to receive messages.
        exchangeKeys();
        showMessage("\n Streams are now setup. You can begin your conversation now.");
    }

    //during converation this will be what is running.
    private void whileConnectedDoChat() throws IOException{
        String payload = "You are now connected";
        sendMessage(payload);
        ableToType(true);

        do {//this is where the magic happens
            try {
                boolean isImage = input.readBoolean();
                ImageIcon image;
                if (isImage) {
                    image = (ImageIcon) input.readObject();

                    // send Client's message with an image
                    showMessage("\n" + clientName +  " - ");
                    showIconOnChatWindow(image);
                }
                else {
                    showMessage("\n" + myEncryptor.getDecryptedMessage((byte[]) input.readObject()));
                }
            } catch (ClassNotFoundException classNotFoundException){
                showMessage("\n Unknown Object Type.");
            }
        } while (!payload.equals("CLIENT - END"));
    }

    private void closeProgramDown(){//close streams and clean things up.
        showMessage("\n Closing Connections Down...");
        ableToType(false); //ability to type is turned off again.
        try {
            output.close();//close output stream
            input.close();//close input stream
            connection.close();//close the connection between computers
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
        }
    }

    //this will handle sending messages to the client.
    private void sendMessage(String payload) {
        try {
            output.writeBoolean(false);
            output.writeObject(myEncryptor.encryptString(name + " - " + payload, clientKey));
            output.flush();

            showMessage("\n" + name + " - "+ payload);
        } catch (IOException ioException) {
            appendString("\n An error has occurred while send a message");
        }
    }

    //updates ONLY the chatWindow
    private void showMessage(final String payload) {
        //Change the text that appears in the chat window. We only want to update the window
        SwingUtilities.invokeLater(//uses a thread to add a single line of code the end of the chat window
                () -> appendString(payload)
        );
    }

    private void sendImage(ImageIcon imageToSend) {
        try {
            output.writeBoolean(true);
            output.writeObject(imageToSend);
            output.flush();

            showMessage("\n" + name + " - ");
            showIconOnChatWindow(imageToSend);
        }
        catch (IOException e) {
            appendString("\n ERROR: IMAGE UNABLE TO BE SENT");
        }
    }

    private void showIconOnChatWindow(final ImageIcon icon) {
        SwingUtilities.invokeLater(
                () -> ChatWindow.insertIcon(getScaledIcon(icon))
        );
    }

    private ImageIcon getScaledIcon(ImageIcon icon) {
        double scaleFactor = 200.0 / icon.getIconHeight();
        Image scaledImage = icon.getImage().getScaledInstance((int)(scaleFactor * icon.getIconWidth()), 200, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    //prevent typing if there is no connection.
    private void ableToType(final boolean bool) {
        SwingUtilities.invokeLater(//uses a thread to add a single line of code the end of the chat window
                () -> enableOrDisableUIElements(bool)
        );
    }

    private void enableOrDisableUIElements(boolean enabled) {
        imageButton.setEnabled(enabled);
        userText.setEditable(enabled);
    }

    private void exchangeKeys() {
        try {
            Object keyObject = input.readObject();
            if (keyObject instanceof PublicKey) {
                clientKey = (PublicKey) keyObject;
            }
            output.writeObject(myEncryptor.getPublicKey());
        }
        catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception reading key object: " + e.getStackTrace());
        }
    }

    private void appendString(String str) {
        StyledDocument styledDocument = ChatWindow.getStyledDocument();
        try {
            styledDocument.insertString(styledDocument.getLength(), str, null);
        }
        catch (BadLocationException e) {
            System.out.println("Exception appending string: " + e.getStackTrace());
        }
    }

    private String openImageDialogAndReturnPath() {
        JFrame imageDialogFrame = new JFrame();
        JFileChooser imageChooser = new JFileChooser();
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
                "JPG, JPEG, & PNG images", "jpg", "jpeg", "png");
        imageChooser.setFileFilter(imageFilter);

        switch (imageChooser.showOpenDialog(imageDialogFrame)) {
            case JFileChooser.APPROVE_OPTION:
                System.out.println("This image opened: " + imageChooser.getSelectedFile().getName());
                return imageChooser.getSelectedFile().getPath();
            case JFileChooser.CANCEL_OPTION:
                System.out.println("Open image operation cancelled.");
                break;
        }
        return null;
    }

}//end of class
