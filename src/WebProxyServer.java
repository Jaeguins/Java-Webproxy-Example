

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WebProxyServer{
    public ServerSocket serverSocket;
    public boolean serverWaiting=true;
    Queue<Reactor> queue= new LinkedList<>();
    public boolean isEmpty(){
        return queue.isEmpty();
    }
    public Reactor pop(){
        return queue.poll();
    }
    public WebProxyServer(int port){
        try {
            serverSocket=new ServerSocket(port);
            turnOn();
        } catch (IOException e) {e.printStackTrace();}
    }
    public void turnOn(){
        serverWaiting=true;
        while(serverWaiting){
            try {
                LogStation.log("server","listening ready.");
                Reactor handler=new Reactor(serverSocket.accept());
                queue.offer(handler);
            }catch(IOException e){e.printStackTrace();}
            if(!isEmpty()){
                Reactor popped=pop();
                new Thread(popped).start();
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
    public static void main(String args[]){
        Scanner scan=new Scanner(System.in);
        System.out.println("$Input port : ");
        WebProxyServer w=new WebProxyServer(scan.nextInt());
    }
}
class Reactor implements Runnable{
    String sub="";
    Socket sock;
    public Reactor(Socket socket){
        this.sock=socket;
        sub+=socket.getInetAddress();
        LogStation.log(sub,"connected");
    }
    @Override
    public void run() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String poppedMsg = reader.readLine();
            String data = "";
            String slaveData = "";
            byte[] slaveBuf;
            while (poppedMsg!=null&&!poppedMsg.equals("")) {
                data += poppedMsg + "\n";
                poppedMsg = reader.readLine();
            }
            HTTPReceiver parser = new HTTPReceiver(data);
            String localPath=("./caches/"+parser.target.substring(7).replace('?','/').replace('\\','/')+".cache");
            File cache=new File(localPath);
            Path pathToFile=Paths.get(localPath);
            Files.createDirectories(pathToFile.getParent());
            if(!cache.isFile()){
                String host=new URL(parser.target).getHost();
                URL url=new URL(parser.target);
                URLConnection urlConn=url.openConnection();
                urlConn.setDoOutput(true);
                urlConn.setDoInput(true);
                urlConn.setUseCaches(true);
                List<String> keyList = new ArrayList<>(parser.field.keySet());
                List<String> valueList = new ArrayList<>(parser.field.values());
                for(int i=0;i<keyList.size();i++){
                    urlConn.setRequestProperty(keyList.get(i),valueList.get(i));
                }
                InputStream in=urlConn.getInputStream();
                slaveBuf=in.readAllBytes();
                LogStation.log(host, parser.target);
                FileOutputStream fio=new FileOutputStream(localPath);
                fio.write(slaveBuf);
                fio.close();
            }else {
                slaveBuf=new FileInputStream(cache).readAllBytes();
                LogStation.log("file", localPath);
            }
            OutputStream masterOut=sock.getOutputStream();

            masterOut.write(slaveBuf);
            LogStation.log("proxyOut",localPath);
            reader.close();
            masterOut.flush();
            masterOut.close();
            sock.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
class LogStation{//logger
    public static void log(String sub,String msg){
        System.out.println("$ "+sub+" :"+msg);
    }
}

class HTTPReceiver{
    String info;
    String target;
    String version;
    public LinkedHashMap<String,String> field=new LinkedHashMap<>();
    public HTTPReceiver(String input){
        String[] lines=input.split("\n");
        String[] header=lines[0].split(" ");
        info=header[0];
        target=header[1];
        version=header[2];
        for(int i=1;i<lines.length;i++) {
            String[] tmp = lines[i].split(": ");
            field.put(tmp[0], tmp[1]);
        }
    }
    public String toPacket(){
        StringBuilder stringBuilder=new StringBuilder();
        String space=" ";
        String line="\r\n";
        String diff=": ";
        stringBuilder.append(info);
        stringBuilder.append(space);
        stringBuilder.append(target.substring(target.indexOf('/',8)));
        stringBuilder.append(space);
        stringBuilder.append(version);
        stringBuilder.append(line);
        List<String> keyList = new ArrayList<>(field.keySet());
        List<String> valueList = new ArrayList<>(field.values());
        for(int i=0;i<keyList.size();i++){
            stringBuilder.append(keyList.get(i));
            stringBuilder.append(diff);
            stringBuilder.append(valueList.get(i));
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }
}

