import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiChatServer {
    Map<String, DataOutputStream> clients;

    public MultiChatServer() {
        this.clients = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        new MultiChatServer().start();
    }
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("start server");

            while (true) {
                // 접속한 클라이언트 소켓의 정보
                Socket socket = serverSocket.accept();
                System.out.println(socket.getInetAddress() + ":" + socket.getPort() + " connect");

                ServerReceiver thread = new ServerReceiver(socket);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ServerReceiver extends Thread {
        Socket socket;
        DataInputStream in;
        DataOutputStream out;
        String name;

        public ServerReceiver(Socket socket) {
            this.socket = socket;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                name = in.readUTF();
                if(clients.get(name) != null) {
                    out.writeUTF("이미 존재하는 이름 : " + name);
                    out.writeUTF("다른 이름을 사용하세요.");
                    System.out.println(socket.getInetAddress() + ":" + socket.getPort() + " disconnect");
                    in.close();
                    out.close();
                    socket.close();
                    socket = null;
                } else {
                    sendToAll(name + " 입장");
                    clients.put(name, out);
                    while(in != null) {
                        sendToAll(in.readUTF());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(socket != null) {
                    sendToAll(name + " 퇴장");
                    clients.remove(name);
                    System.out.println(socket.getInetAddress() + ":" + socket.getPort() + " disconnect");
                }
            }
        }
    }

    private void sendToAll(String msg) {
        Iterator iterator = clients.keySet().iterator();
        while (iterator.hasNext()) {
            try {
                // 각 클라이언트의 DataOutputStream 얻어와서 출력
                DataOutputStream out = clients.get(iterator.next());
                out.writeUTF(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
