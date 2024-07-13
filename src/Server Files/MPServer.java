import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;

/**
 *  Class for MPServer
 */
public class MPServer implements Runnable {
    private ArrayList<ServerClientHandler> clients;
    private ServerSocket serverSocket;
    private Boolean serverExit;
    private ExecutorService Threadpool;

    /**
     * Method: ServerShutdown()
     *        - Initiates the shutdown process for the server.
     *        - Closes the server socket and disconnects all connected client handlers.
     */
    public void ServerShutdown() {
        // Set the serverExit flag to indicate a shutdown request
        this.serverExit = true;
        try {
            // Check if the server socket is not closed before attempting to close it
            if (!serverSocket.isClosed()) {
                serverSocket.close(); // Close the server socket
            }

            // Iterate through all connected client handlers and initiate their disconnection
            for (ServerClientHandler SCH : this.clients) {
                SCH.disconnect();
            }
        } catch (Exception e) {
            // Handle exceptions related to the server shutdown process
            System.out.println("Error in shutting down the server :(");
        }
    }

    /**
     * Constructor for MPServer
     */
    public MPServer () {
        this.clients = new ArrayList<>();
        this.serverExit = false;
    }

    /**
     * Method: run()
     *        - Initiates the server and listens for incoming client connections.
     *        - Creates a thread pool to handle multiple clients concurrently.
     */
    public void run() {
        int PortNumber = 12345; // Desired port number

        Socket serverEndpointForClient;
        this.Threadpool = Executors.newCachedThreadPool();

        try {
            // Create a server socket to listen for incoming client connections on the specified port
            this.serverSocket = new ServerSocket(PortNumber);

            // Continue listening for incoming connections until a shutdown signal is received
            while (!this.serverExit) {
                System.out.println("Listening on port " + PortNumber + "...");

                // Accept a client connection and obtain the server endpoint for the client
                serverEndpointForClient = serverSocket.accept();

                // Create a new instance of ServerClientHandler for the connected client
                ServerClientHandler clientHandler = new ServerClientHandler(serverEndpointForClient);

                // Add the client handler to the list of connected clients
                clients.add(clientHandler);

                // Execute the client handler in a separate thread using the thread pool
                this.Threadpool.execute(clientHandler);
            }

        } catch (Exception e) {
            // Handle exceptions related to the server operation by initiating server shutdown
            this.ServerShutdown();
        }
    }


    /**
     * ServerClientHandler is a class to handles Client inputs/commands
     */
    public class ServerClientHandler implements Runnable {
        private Socket Client_Socket;
        private String nickname = "User";   //Default username
        private BufferedReader in;
        private PrintWriter out;
        private boolean isRegistered;

        //HERE: consider variables for file sending (File Input and Output reader)

        /**
         * Constructor for Server Client Handler
         *
         * @param clientSocket: The Socket representing the client's connection to the server.
         */
        public ServerClientHandler(Socket clientSocket) {
            // Set the client socket and initialize the nickname based on the number of connected clients
            this.Client_Socket = clientSocket;
            this.nickname = this.nickname + (clients.size() + 1);
            this.isRegistered = false;

            try {
                // Initialize input and output streams for communication with the client
                this.in = new BufferedReader(new InputStreamReader(Client_Socket.getInputStream()));
                this.out = new PrintWriter(Client_Socket.getOutputStream(), true);
            } catch (Exception e) {
                // Handle exceptions related to stream initialization
                System.out.println("Error in initializing streams for client " + this.nickname);
            }
        }

        /**
         * Method: sendMessage(String message)
         * -Sends a message to the connected client using the output stream.
         *
         * @param message: The message to be sent to the client.
         */
        public void sendMessage(String message) {
            this.out.println(message);
        }

        /**
         * Method: broadcastToEveryone(String msg)
         * - Broadcasts a message to all connected clients by iterating through the client handlers.
         *
         * @param msg: The message to be broadcasted.
         */
        public void broadcastToEveryone(String msg) {
            // Iterate through all connected client handlers and check if not null
            for (ServerClientHandler SCH : clients) {
                if (SCH != null) SCH.sendMessage(msg);
            }
        }

        /**
         * Method: SendUnicastToSomeone(String Alias, String msg)
         * - Sends a unicast message to a specific client identified by their alias.
         *
         * @param alias: The alias of the target client to receive the unicast message.
         * @param msg:   The message to be sent in the unicast.
         */
        public void SendUnicastToSomeone(String alias, String msg) {
            // Iterate through all connected client handlers and check if exists
            for (ServerClientHandler SCH : clients) {
                if (SCH.nickname.equals(alias)) {
                    // Send the unicast message to the target client
                    SCH.sendMessage("From " + this.nickname + ": " + msg);
                }
            }
        }

        /**
         * Method: AliasExists(String Alias)
         * - Check if alias exists within the list of Clients
         *
         * @param alias: The alias of the target client to receive the unicast message.
         */
        public boolean AliasExists(String alias) {
            //Iterates through the list of Clients and check if exists
            for (ServerClientHandler SCH : clients) {
                if (SCH.nickname.equals(alias)) return true;
            }
            return false;
        }

        @Override
        public void run() {
            try {
                String inputLine;
                System.out.println(this.nickname + " joined.");
                out.println("Connection to the File Exchange Server is successful!");
                this.broadcastToEveryone("Welcome "+this.nickname);
                // Read input until the termination message is received
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from client " + Client_Socket.getInetAddress().getHostAddress() + ": " + inputLine);

                    // Check for termination message

                    //Handle '/leave' command from client
                    if (inputLine.equals("/leave")) {
                        out.println("Connection closed. Thank you!");
                        //Send message to everyone
                        broadcastToEveryone(this.nickname + " left the file exchange server.");
                        System.out.println(this.nickname + " left the file exchange server.");
                        // Disconnets from the server
                        this.disconnect();
                        break;
                    }
                    // Handle '/?' command which displays the commands
                    else if (inputLine.equals("/?")) {
                        questionmark();
                    }
                    // Handle '/register' command  which allow the user to register to server and change alias
                    else if (ExtractNthWord(inputLine, 1).equals("/register")) {
                        // If no nickname inputted
                        if (inputLine.equals("/register") || inputLine.equals("/register ")) {
                            sendMessage("REGISTER_FALSE");
                            out.println("No inputted nickname. Please provide one.");
                        } else {
                            // If alias exists
                            if (AliasExists(ExtractNthWord(inputLine, 2))) {
                                sendMessage("REGISTER_FALSE");
                                out.println("Nickname already exists. Please use a different one.");
                            }
                            // When nickname does not exist
                            else {
                                this.isRegistered = true;
                                sendMessage("REGISTER_TRUE");
                                //Broadcast to every client about changing nickname of one user
                                broadcastToEveryone(this.nickname + " changed their alias to: " + ExtractNthWord(inputLine, 2));

                                //Server console print about change nickname
                                System.out.println(this.nickname + " changed their alias to: " + ExtractNthWord(inputLine, 2));
                                out.println("Successfully changed your nickname to: " + ExtractNthWord(inputLine, 2));
                                this.nickname = ExtractNthWord(inputLine, 2);
                            }
                        }
                    }
                    // User did not regitser
                    else if (this.isRegistered == false) {
                        out.println("Cannot process the command. Please register first.");
                    }
                    //Handle '/store' command from Client which allow the client to store file in the server
                    else if (ExtractNthWord(inputLine, 1).equals("/store")) {
                        String filename = ExtractNthWord(inputLine, 2);
                        // Store file in the server
                        storeFile(filename);
                    }
                    // Handle '/dir' command from client and allow to display the files from server directory
                    else if (ExtractNthWord(inputLine, 1).equals("/dir")) {
                        // Display the files under server directory
                        displayDirectory();
                    }
                    // Handle '/get' command from client, and this allow to get a file from the server directory
                    else if (ExtractNthWord(inputLine, 1).equals("/get")) {
                        String filename = ExtractNthWord(inputLine, 2);
                        // Open directory
                        File file = new File("./" + filename);
                        // Check if file exists
                        if (file.exists()) {
                            sendMessage("FILE_EXISTS");
                            out.flush();
                            out.println(filename);
                            out.println(file.length());
                            // Get File from the Server
                            getFile(inputLine);
                            out.flush();
                        } else {
                            // When file does not exist
                            sendMessage("FILE_FALSE");
                            out.println("Server: Sorry, file not found!");
                        }

                    }
                    // Handle '/chat' feature which allow client to message everyone (Additional Feature)
                    else if (ExtractNthWord(inputLine, 1).equals("/chat")) {
                        broadcastToEveryone(this.nickname + ": " + inputLine.substring(6));
                    }
                    // Handle '/chatuni' which allow client to message a specific client (Additional Feature)
                    else if (ExtractNthWord(inputLine, 1).equals("/chatuni")) {
                        // Check if Client exists within the list of Clients
                        if (AliasExists(ExtractNthWord(inputLine, 2))) {
                            String MessageToSend = Arrays.stream(inputLine.split("\\s+"))
                                    .skip(2).reduce((word1, word2) -> word1 + " " + word2).orElse("");
                            SendUnicastToSomeone(ExtractNthWord(inputLine, 2), MessageToSend);
                        }
                        // When Alias does not exist
                        else sendMessage("Sorry, inputted user alias does not exist. Try again.");
                    }
                    // Invalid Input
                    else {
                        out.println("Improper input. Please type \"/?\" For a format of how to request from the server.");
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
                // Disconnects when error exists
                this.disconnect();
            }
        }

        /**
         * Method questionmark()
         * - Displays all the commands
         */
        public void questionmark() {
            out.println("Here are the list of commands you can request to the server:");
            out.println("To leave the server: \"/leave\"");
            out.println("To change your alias: \"/register <Insert your new alias here>\"");
            out.println("To send your file: \"/store <Insert your file's name here>\"");
            out.println("To request the files in store: \"/dir\"");
            out.println("To request a file: \"/get <Insert your targeted file name here (given proper file extension)>\"");
            out.println("To give a message to everyone: \"/chat <Insert your message here>\"");
            out.println("To give a message to one person: \"/chat <username> <Insert your message here>\"");
            out.println("To see this again: \"/?\"");
        }

        /**
         * Method: disconnect()
         * - Disconnect Client to the server upon reading command '/leave' from client
         */
        public void disconnect() {
            try {
                // Close the Socket from a client
                this.in.close();
                this.out.close();
                if (!this.Client_Socket.isClosed()) this.Client_Socket.close();
            } catch (Exception e) {
                System.out.println("Error in disconnecting the client " + this.nickname);
            }
        }

        /**
         * Method: storeFile(String filename)
         * - Receives a file from the client and stores it on the server.
         *
         * @param filename: The name of the file to be stored on the server.
         */
        private void storeFile(String filename) throws IOException {
            try {
                // Read the size of the file from the client
                long sizeOfFile = Long.parseLong(in.readLine());

                // Create a DataInputStream to read data from the client's input stream
                DataInputStream fileInput = new DataInputStream(Client_Socket.getInputStream());

                // Create a FileOutputStream to write the received data to a file
                FileOutputStream fileOutput = new FileOutputStream(filename);

                // Buffer to hold data read from the input stream
                byte[] buffer = new byte[1];

                int bytesRead;

                // Loop through the file data, reading and writing in chunks
                while ((bytesRead = fileInput.read(buffer)) != -1 && sizeOfFile > 0) {
                    // Write the buffer to the file
                    fileOutput.write(buffer);

                    // Update the remaining size of the file
                    sizeOfFile -= bytesRead;

                    // Break out of the loop if the entire file has been received
                    if (sizeOfFile <= 0) break;
                }

                // Close the FileOutputStream
                fileOutput.close();

                // Output a success message indicating the received file and sender
                System.out.println("File received from " + this.nickname + ": " + filename);

                // Send a confirmation message back to the client indicating the successful upload
                sendMessage(this.nickname + " <" + getCurrentTimestamp() + ">: Uploaded " + filename);

            } catch (IOException e) {
                // Handle exceptions related to file reception errors and print an error message
                System.out.println("Error receiving file from " + this.nickname + ": " + e.getMessage());
            }
        }

        /**
         * Method: getFile(String command)
         * - Retrieves a file from the server and sends it to the client.
         *
         * @param command: The command containing information about the requested file.
         */
        private void getFile(String command) {
            try {
                // Extract the filename from the command
                String filename = ExtractNthWord(command, 2);

                // Create a File object representing the requested file
                File file = new File("./" + filename);

                // Create a DataOutputStream to send data to the client's output stream
                DataOutputStream fileWriter = new DataOutputStream(Client_Socket.getOutputStream());

                // Create a FileInputStream to read data from the file
                FileInputStream fileReader = new FileInputStream("./" + filename);

                // Buffer to hold data read from the file
                byte[] buffer = new byte[1];

                // Initialize the size of the file to be sent
                long sizeOfFile = file.length();

                int bytesRead;

                // Loop through the file data, reading and writing in chunks
                while ((bytesRead = fileReader.read(buffer)) != -1 && sizeOfFile > 0) {
                    // Write the buffer to the client's output stream
                    fileWriter.write(buffer);
                    // Update the remaining size of the file
                    sizeOfFile -= bytesRead;
                }

                // Flush the DataOutputStream to ensure all data is sent and close FileInputStream
                fileWriter.flush();
                fileReader.close();

            } catch (Exception e) {
                // Handle exceptions related to file sending errors and print an error message
                System.out.println("Error in sending file.");
            }
        }

        /**
         * Method: sendDirectoryToClient()
         * - Retrieves the list of files in the server's current directory and sends it to the client.
         */
        public void sendDirectoryToClient() {
            // Get the current directory path
            String currentDirectory = System.getProperty("user.dir");

            // Create a File object representing the current directory
            File directory = new File(currentDirectory);

            // StringBuilder to store information about the server directory
            StringBuilder directoryInfo = new StringBuilder();

            // Check if the server directory exists
            if (!directory.exists()) {
                sendMessage("Server directory does not exist.");
                return;
            }

            // Retrieve the list of files in the server directory
            File[] files = directory.listFiles();

            // Build a string with information about the server directory
            if (files != null && files.length > 0) {
                directoryInfo.append("Server Directory:\n");

                // Append the names of each file in the directory to the StringBuilder
                for (File file : files) {
                    directoryInfo.append(file.getName()).append("\n");
                }
            } else {
                directoryInfo.append("Server directory is empty.\n");
            }

            // Send the directory information to the client
            sendMessage(directoryInfo.toString());
        }

        /**
         * Method: displayDirectory()
         * - Initiates the process of sending the server directory information to the client.
         */
        private void displayDirectory() {
            try {
                // Call sendDirectoryToClient to retrieve and send the server directory information
                sendDirectoryToClient();
                // Flush the PrintWriter to ensure the information is sent to the client
                out.flush();
            } catch (Exception e) {
                // Handle exceptions related to sending server directory information
                out.println("Error reading server directory: " + e.getMessage());
                out.flush();
            }
        }

        /**
         * Public static method: ExtractNthWord(String input, int nth)
         * - Extracts the nth word from a given input string based on space separation.
         *
         * @param input: The input string from which to extract the word.
         * @param nth:   The position of the word to be extracted.
         * @return The nth word from the input string.
         */
        public static String ExtractNthWord(String input, int nth) {
            // Split the input string into an array of words based on space separation
            String[] words = input.split(" ");
            return words[nth - 1];
        }

        /**
         * Private method: getCurrentTimestamp()
         * - Generates and returns the current timestamp in the "yyyy-MM-dd HH:mm:ss" format.
         *
         * @return The current timestamp as a formatted string.
         */
        private String getCurrentTimestamp() {
            // Create a SimpleDateFormat object with the desired date-time format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date());
        }
    }

    /**
     * Main Driver of the class
     * @param args
     */
    public static void main(String[] args) {
        MPServer Server = new MPServer();
        Server.run();
    }
}
