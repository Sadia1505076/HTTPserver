package httpserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class HTTPserver {

    static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        int id = 1;
        ServerSocket serverConnect = new ServerSocket(PORT);
        System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
        while (true) {
            Socket s = serverConnect.accept();
            Worker wt = new Worker(s, id);
            Thread t = new Thread(wt);
            t.start();
            System.out.println("Client " + id + " connected to server");
            id++;

        }
    }

}

class Worker implements Runnable {

    private Socket connectionSocket;
    private int id;

    public Worker(Socket s, int id) {
        this.connectionSocket = s;
        this.id = id;
    }

    public void run() {

        //while (true) {
        String clientSentence, serverSentence, clientSentenceGet, errorcode;
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            PrintWriter outToClient = new PrintWriter(connectionSocket.getOutputStream());
            clientSentence = inFromClient.readLine();

            System.out.println("Here Input : " + clientSentence);

            if (clientSentence != null) {
                String spilts[] = clientSentence.split("/");

//                for (int i = 0; i < spilts.length; i++) {
//                    System.out.println(spilts[i]);
//                }
                if (spilts[0].equals("GET ")) {
                    //System.out.println("get");
                    String nameofthefile[] = spilts[1].split(" ");

                    if (nameofthefile[0].length() == 0) {
                        nameofthefile[0] = "index.html";
                    }
                    //System.out.println(nameofthefile[0]);
                    //System.out.println(nameofthefile[1]);

                    //try {
                    File file = new File(nameofthefile[0]);
                    if (!file.exists()) {
                        System.err.println("404 NOT FOUND");
                        errorcode = "HTTP/1.1 404 NOT FOUND";

                        nameofthefile[0] = "notfoundpage.html";
                        // System.out.println("err is:"+nameofthefile[0]);
                        File newfile = new File(nameofthefile[0]);
                        get_the_file(newfile, nameofthefile[0], outToClient, connectionSocket, errorcode);
                        //System.out.println("func called!");
                    } else {
                        errorcode = "HTTP/1.1 200 OK";
                        get_the_file(file, nameofthefile[0], outToClient, connectionSocket, errorcode);

                    }

                } else if (spilts[0].equals("POST ")) {
                    System.out.println("post");
                    String postlines = inFromClient.readLine();
                    //inFromClient.re 
                    int postlen = 0;
                    while (postlines != null && (postlines.length() != 0)) {
                        System.out.println("input:" + postlines);
                        if (postlines.indexOf("Content-Length:") != -1) {
                            postlen = Integer.parseInt(postlines.substring(postlines.indexOf("Content-Length:") + 16, postlines.length()));

                        }
                        postlines = inFromClient.readLine();
                    }
                    System.out.println("len is:" + postlen);
                    if (postlen > 0) {
                        char[] data = new char[postlen];
                        inFromClient.read(data, 0, postlen);
                        String stringdata = "";
                        stringdata = new String(data);
                        System.out.println("data is " + stringdata);
                        get_for_post(outToClient, stringdata);

                    }

                }
            }

        } catch (Exception e) {

        }
        //}
    }

    private static String get_MIME_type(String nameofhefile) {
        if (nameofhefile.endsWith("html")) {
            return "text/html";
        } else if (nameofhefile.endsWith("pdf")) {
            return "application/pdf";
        } else if (nameofhefile.endsWith("jpg")) {
            return "image/jpg";
        } else if (nameofhefile.endsWith("jepg")) {
            return "image/jepg";
        } else if (nameofhefile.endsWith("png")) {
            return "image/png";
        } else if (nameofhefile.endsWith("gif")) {
            return "image/gif";
        } else if (nameofhefile.endsWith("tiff")) {
            return "image/tiff";
        } else if (nameofhefile.endsWith("tiff")) {
            return "image/tiff";
        } else if (nameofhefile.endsWith("txt")) {
            return "text/plain";
        }
        return null;
    }

    private static void get_the_file(File file, String nameofthefile, PrintWriter outToClient, Socket connectionSocket, String errorcode) {
        try {
            System.out.println("name of the file:" + nameofthefile);
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            OutputStream os = connectionSocket.getOutputStream();
            byte[] contents;
            long fileLength = file.length();

            System.out.println(ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
            String date = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
            String modifiedDate = date.substring(0, 24);

            modifiedDate = modifiedDate + " GMT";
            System.out.println(modifiedDate);

            String contentType = get_MIME_type(nameofthefile);

            String header = errorcode + "\n" + "MIME-version: 1.1\n"
                    + "Date: " + modifiedDate + "\n"
                    + "Server: NaviServer/2.0 AOLserver/2.3.3\n"
                    + "Content-Type:" + contentType + "\n" + "Content-Length: " + fileLength + "\n\n";
            System.out.println("header of response is: " + header);
            outToClient.println(header);
            outToClient.flush();
            long current = 0;

            long start = System.nanoTime();
            while (current != fileLength) {
                int size = 10000;
                if (fileLength - current >= size) {
                    current += size;
                } else {
                    size = (int) (fileLength - current);
                    current = fileLength;
                }
                contents = new byte[size];
                bis.read(contents, 0, size);
                os.write(contents);
                //outToClient.println(contents);
                //System.out.println("Sending file ... "+(current*100)/fileLength+"% complete!");
            }
            os.flush();
            System.out.println("File sent successfully!");
        } catch (Exception e) {
            System.out.println("cant transfer file!" + e.toString());
        }

    }

    private static void get_for_post(PrintWriter outToClient,String postdata) {
        String date = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String modifiedDate = date.substring(0, 24);

        modifiedDate = modifiedDate + " GMT";
        String header = "HTTP/1.1 200 OK" + "\n" + "MIME-version: 1.1\n"
                + "Date: " + modifiedDate + "\n"
                + "Server: NaviServer/2.0 AOLserver/2.3.3\n"
                + "Content-Type:" +"text/html"  + "\n\n";
        System.out.println("header of response is: " + header);
        
        String dataarray[]=postdata.split("=");
        System.out.println(dataarray[1]);
        
        outToClient.println(header);
        outToClient.flush();
        outToClient.println("<h1> Welcome to CSE 322 Offline 1</h1>");
        outToClient.flush();
        outToClient.println("<h2> HTTP REQUEST TYPE-> </h2>");
        outToClient.flush();
        outToClient.println("<h2> Post->"+" "+dataarray[1].replace('+',' ') +"</h2>");
        outToClient.flush();
        outToClient.println("<form name=\"input\" action=\"http://localhost:8080/form_submited\" method=\"post\">");
        outToClient.flush();
        outToClient.println("Your Name:");
        outToClient.flush();
        outToClient.println("<input type=\"text\" name=\"user\">");
        outToClient.flush();
        outToClient.println("<input type=\"submit\" value=\"Submit\">");
        outToClient.flush();

    }
}
