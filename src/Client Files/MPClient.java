
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.io.*;

/**
 *  Class for MPClient
 */
public class MPClient implements Runnable {

    private Boolean ClientisConnectedtoAServer;
    private Boolean exit;
    private Socket Clientsocket;
    private PrintWriter out;
    private BufferedReader serverReader;
    private boolean isRegistered;
    private volatile boolean ReadFromHandler;
    private ClientInputHandler CIHandler;

    /**
     * Constructor for MPClient Class
     *      - Initialize boolean variables to false
     */
    public MPClient () {
        this.ClientisConnectedtoAServer = false;
        this.exit = false;
        this.isRegistered = false;
        this.ReadFromHandler = false;
    }

    /**
     * Method: Run()
     *       - Implementing the behavior when the Client is running
     */
    public void run() {
        try {
            // Set-up scanner to read input
            Scanner s = new Scanner(System.in);
            String input;

            // Continue running until the Client is connected and the program terminated
            while (!ClientisConnectedtoAServer && !exit) {
                try {
                    System.out.print("Input: ");
                    input = s.nextLine();

                    // Handle '/join' command which allow the user to join the server
                    if (ExtractNthWord(input, 1).equals("/join")) {
                        //Establish a connection to the server
                        this.Clientsocket = new Socket(ExtractNthWord(input, 2), Integer.parseInt(ExtractNthWord(input, 3)));
                        this.out = new PrintWriter(this.Clientsocket.getOutputStream(), true);
                        this.serverReader = new BufferedReader(new InputStreamReader(this.Clientsocket.getInputStream()));

                        // Set up a handler for incoming server messages
                        this.CIHandler = new ClientInputHandler(this.Clientsocket);
                        Thread CIHandlerThread = new Thread(this.CIHandler);
                        CIHandlerThread.start();
                        this.ClientisConnectedtoAServer = true;
                    }

                    //Handle '/exit' command which terminates the program (Client Side)
                    else if (ExtractNthWord(input, 1).equals("/exit")) {
                        this.exit = true;
                        System.out.println("Exiting...");
                    }

                    // Allows to show the different commands
                    else if (ExtractNthWord(input, 1).equals("/?")) {
                        //put logic here for /?
                        System.out.println("The current commands you can put are: ");
                        System.out.println("To join a server: \"/join <server_ip_add> <port>\"");
                        System.out.println("To exit the client: \"/exit\"");
                        System.out.println("To see this again: \"/?\"");
                    }
                    // Invalid Input
                    else {
                        System.out.println("Input failed. Please type \'/?\' to see the possible commands");
                    }
                } catch (Exception e) { //if inputted socket or ip doesn't work :(
                    System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number..");
                }
            }

            // Continue running until the Client is not existing
            while (!this.exit) {
                if (!ReadFromHandler) {
                    // Check if reading from the handler is enabled
                    String serverResponse = serverReader.readLine();
                    // If client successfully registered
                    if (serverResponse.equals("REGISTER_TRUE")) {
                        this.isRegistered = true;
                        System.out.println("User is now currently registered. " + this.isRegistered);
                    }
                    // If client wasn't able to successfully registered
                    else if (serverResponse.equals("REGISTER_FALSE")) {
                    }
                    // If file exists within the Client Directory
                    else if (serverResponse.equals("FILE_EXISTS")) {
                        System.out.println("File exists!");

                        // Reads File
                        String filename = serverReader.readLine();
                        System.out.println("The file name is: " + filename);
                        getFile(filename);
                    }
                    // If file does not exists
                    else if (serverResponse.equals("FILE_FALSE")) {
                    }
                    else if (serverResponse != null) System.out.println(serverResponse);
                    else break;
                }
                else {
                    continue;
                }
            }
            // If there's an error with the Client Side
        } catch (IOException e) {
            System.out.println(e);
            disconnect();
            System.out.println("Client side error. Exiting the program...");
        }
    }

    /**
     * Method: disconnect ()
     *      - If the user chose '/leave' command
     *      - This method allows the user to disconnect from the server
     */
    public void disconnect() {
        this.ClientisConnectedtoAServer = false;
        this.exit = true;
        try {
            this.out.close();
            this.serverReader.close();
            if(!this.Clientsocket.isClosed()) {
                this.Clientsocket.close();
            }
        } catch (Exception e) {
            System.out.println("Error: Disconnection failed. Please Connect to the Server first");
        }
    }

    /**
     * This Class handle the input commands of the client
     */
    public class ClientInputHandler implements Runnable {

        private Socket CSocket;

        /**
         * Constructor for ClientInputHandler
         *
         * @param ClientSocket : BufferReader for Server input
         */
        public ClientInputHandler (Socket ClientSocket) {
            this.CSocket = ClientSocket;
        }

        /**
         * Method: Run()
         *       - Implementing the behavior when the Client is running once the user registered
         */
        public void run() {
            try {
                System.out.println("CIH is now running: ");
                // Scanner for inputting commands
                Scanner sCIH = new Scanner(System.in);
                String inputmsg;

                // Continue receiving input until the clients exits
                while (!exit) {
                    System.out.print("Input: ");
                    inputmsg = sCIH.nextLine();
                    // Handle '/leave' command which disconnects the user from the server
                    if (inputmsg.equals("/leave")) {
                        out.println(inputmsg);
                        disconnect();
                    }
                    // Handle '/?' command which allow the client to view the commands
                    else if (inputmsg.equals("/?")) {
                        out.println(inputmsg);
                    }
                    //Handle '/register' command which allow the user to register within the server
                    // This also allows the user to have their Alias
                    else if(ExtractNthWord(inputmsg,1).equals("/register")) {
                        ReadFromHandler = true;
                        out.println(inputmsg);
                        out.flush();
                        ReadFromHandler = false;
                    }
                    // If the clients haven't registered yet
                    else if (isRegistered == false) {
                        System.out.println("Please register first so that you can access the commands.");
                    }
                    // Handle '/store' command which allow the client to store the file to server
                    else if (ExtractNthWord(inputmsg,1).equals("/store")) {
                        String filename = ExtractNthWord(inputmsg, 2);
                        File file = new File("./"+ filename);

                        // Check if file exists within the Client's Directory
                        if(file.exists()) {
                            // Send '/store' command to the server
                            out.println(inputmsg);
                            out.flush();
                            // Client is registered
                            if (isRegistered) {
                                out.println(file.length());
                                // Store the file
                                StoreFile(inputmsg);
                            }
                            out.flush();
                        }
                        // File does not found in the directory
                        else {
                            System.out.println("Error: File not found!");
                        }
                    }
                    else {
                        out.println(inputmsg);
                    }
                }
                // Otherwise, disconnects
            } catch (Exception e) {
                disconnect();
            }
        }
    }

    /**
     * Method: StoreFile ()
     *         - If the user chose '/store' command
     *         - Allow the client to store a file from client to server
     *
     * @param command: read the command from input handler
     */
    private void StoreFile(String command) throws IOException {
        try {
            // Extract the filename from the command
            String filename = ExtractNthWord(command, 2);

            // Create a File object representing the file to be stored (located in the client directory)
            File file = new File("./" + filename);

            // Create a DataOutputStream to write data to the server's output stream
            DataOutputStream fileWriter = new DataOutputStream(Clientsocket.getOutputStream());

            // Create a FileInputStream to read data from the client's file
            FileInputStream fileReader = new FileInputStream("./" + filename);

            byte[] buffer = new byte[1];
            long sizeOfFile = file.length();
            int bytesRead;

            // Loop through the file, reading and writing in chunks
            while ((bytesRead = fileReader.read(buffer)) != -1 && sizeOfFile > 0) {
                // Write the buffer to the server's output stream
                fileWriter.write(buffer); // used to be fileWriter.write(buffer, 0, bytesRead);

                // Update the remaining size of the file
                sizeOfFile -= bytesRead;
            }

            // Flush the output stream to ensure all data is sent
            fileWriter.flush();
            // Close the fileReader
            fileReader.close();

        } catch (Exception e) {
            // Handle exceptions related to file transfer errors
            System.out.println("Error in sending file.");
        }
    }

    /**
     * Method: getFile ()
     *      - If the user chose '/getFile' command
     *      - This method allows the client to get a file from the server
     *
     * @param filename: filename from server directory
     */
    private void getFile(String filename) {
        try {
            // Get the size of the file from the server
            long sizeOfFile = Long.parseLong(this.serverReader.readLine());
            System.out.println("Size of file is: " + sizeOfFile);

            // Create a DataInputStream to read data from the server's input stream
            DataInputStream fileInput = new DataInputStream(this.Clientsocket.getInputStream());

            // Create a FileOutputStream to write the received data to a file
            FileOutputStream fileOutput = new FileOutputStream(filename);

            // Buffer to hold data read from the input stream
            byte[] buffer = new byte[1];

            int bytesRead;

            // Loop through the file data, reading and writing in chunks
            while ((bytesRead = fileInput.read(buffer)) != -1 && sizeOfFile > 0) {
                // Write the buffer to the file
                fileOutput.write(buffer); // used to be fileOutput.write(buffer, 0, bytesRead);

                // Update the remaining size of the file
                sizeOfFile -= bytesRead;
                if (sizeOfFile <= 0) break;
            }

            // Close the FileOutputStream
            fileOutput.close();

            // Output a success message
            System.out.println("Successfully received " + filename);

        } catch (IOException e) {
            // Handle exceptions related to file reception errors
            System.out.println("Error receiving file from the server :(");
        }
    }

    /**
     *  Main Driver of the Class
     */
    public static void main(String[] args) {
        MPClient mpClient = new MPClient();
        mpClient.run();
    }

    /**
     * Method: ExtractNthWord()
     *        - This method extracts the nth word from a given input command by removing spaces.
     *
     * @param input: The input command from the client.
     * @param nth: The position of the word to be extracted, starting from 1.
     * @return: The nth word from the input command.
     */
    public String ExtractNthWord(String input, int nth) {
        // Split the input command into an array of words using space as the delimiter
        String[] words = input.split(" ");

        // Return the nth word (adjusted for array indexing starting from 0)
        return words[nth - 1];
    }

}