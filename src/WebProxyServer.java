

import com.oracle.tools.packager.Log;

import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.util.*;

public class WebProxyServer {
    public static void main(String[] args) {
        ServerSocketHandler serverHandler = new ServerSocketHandler(80);
        serverHandler.turnOn();
    }
}
class SocketHandler{
    Socket socket;
    String sub="";
    BufferedReader reader;
    DataOutputStream writer;
    public SocketHandler(Socket socket){
        this.socket=socket;
        sub+=socket.getInetAddress();
        LogStation.log(sub,"connected");
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new DataOutputStream(socket.getOutputStream());
        }catch(IOException e){e.printStackTrace();}
    }
    public void react()throws IOException{
        try {
            String poppedMsg = readLine();
            String data = "";
            while (!poppedMsg.equals("")) {
                LogStation.log(sub, poppedMsg);
                data += poppedMsg + "\n";
                poppedMsg = readLine();
            }
            String[] lines = data.split("\n");
            String[][] headers = new String[lines.length][];
            headers[0] = lines[0].substring(0, lines[0].lastIndexOf("HTTP/1.0")).split(" ");
            for (int i = 1; i < lines.length; i++) {
                headers[i] = lines[i].split(": ");
            }
            byte[] out;

        switch(headers[0][1]){
            case "/":
                notFound();
                break;
            default:
                URL url = new URL("http://"+headers[0][1].substring(1));
                Socket socket=new Socket(url.getHost(),80);
                OutputStream output=socket.getOutputStream();

                output.write(("GET / HTTP/1.1").getBytes());
                LogStation.log("buffer","GET / HTTP/1.1");
                output.write(("HOST: "+headers[0][1].substring(1)).getBytes());
                LogStation.log("buffer","HOST: "+headers[0][1].substring(1));
                for(int i=2;i<lines.length;i++){
                    if(lines[i].substring(0,"Cookie".length()).equals("Cookie"))continue;
                    output.write(lines[i].getBytes());
                    LogStation.log("buffer",lines[i]);
                }
                output.flush();
                streamData(socket.getInputStream());
                break;
        }
        }catch(IOException e){
            e.printStackTrace();
            notFound();
        }
    }
    public String readLine()throws IOException{
        return reader.readLine();
    }
    public void writeData(String input)throws IOException{
        writeData(input.getBytes());
    }
    public void streamData(InputStream stream)throws IOException{
        BufferedReader rd=new BufferedReader(new InputStreamReader(stream));
        String buf=rd.readLine();
        while(!buf.equals("")) {
            writer.writeBytes(buf);
            LogStation.log("buffered msg",buf);
            buf = rd.readLine();
        }
        writer.flush();
    }
    public void writeData(byte[] data)throws IOException{
        writer.writeBytes("HTTP/1.1 200 OK \r\n");
        writer.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
        writer.writeBytes("Content-Length: " + data.length + "\r\n");
        writer.writeBytes("\r\n");
        writer.write(data, 0, data.length);
        writer.writeBytes("\r\n");
        writer.flush();
    }
    public void redirect()throws IOException{
        writer.writeBytes("HTTP/1.1 301 Moved Permanently\r\n");
        writer.writeBytes("Location: http://127.0.0.1/html/index.html\r\n");
        writer.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
        writer.writeBytes("\r\n");
        writer.writeBytes("\r\n");
        writer.flush();
    }
    public void notFound()throws IOException{
        byte[] data="404 Not Found".getBytes();
        writer.writeBytes("HTTP/1.1 404 Not Found\r\n");
        writer.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
        writer.writeBytes("Content-Length: " + data.length + "\r\n");
        writer.writeBytes("\r\n");
        writer.write(data, 0, data.length);
        writer.writeBytes("\r\n");
        writer.flush();
    }

    public void writerClose()throws IOException{
        writer.close();
    }
    public void readerClose()throws IOException{
        reader.close();
    }
    public void close()throws IOException{
        socket.close();
    }
}
class ServerSocketHandler{
    public ServerSocket serverSocket;
    public boolean serverWaiting=true;
    Queue<SocketHandler> queue= new LinkedList<>();
    Thread thread;

    public boolean isEmpty(){
        return queue.isEmpty();
    }
    public SocketHandler pop(){
        return queue.poll();
    }
    public ServerSocketHandler(int port){
        try {
            serverSocket=new ServerSocket(port);
        } catch (IOException e) {e.printStackTrace();}
    }
    public void turnOn(){
        serverWaiting=true;
        while(serverWaiting){
            try {
                SocketHandler handler=new SocketHandler(serverSocket.accept());
                queue.offer(handler);
            }catch(IOException e){e.printStackTrace();}
            if(!isEmpty()){
                SocketHandler popped=pop();
                new Thread(() -> {
                    try{
                        popped.react();
                    }catch(IOException e){e.printStackTrace();}
                }).start();
            }
        }
    }
    public void turnOff(){
        serverWaiting=false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class LogStation{//logger
    public static void log(String sub,String msg){
        System.out.println(sub+" : "+msg);
    }
}
