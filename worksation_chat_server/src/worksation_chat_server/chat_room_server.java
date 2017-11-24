package worksation_chat_server;



import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class chat_room_server {
    private static ServerSocket server_socket = null;
    static int portNo ;
    private static final int total_client_no = 6;
    private static clienthread_extd_thread[] threads = new clienthread_extd_thread[total_client_no];
    private static TreeSet<chat_rooms> chat_rooms = new TreeSet<chat_rooms>();
    
    public static void setVariable(int s)
    {
    	portNo = s;
    }
    public int getVariable()
    {
        return portNo;
    }
    
    public static void main(String args[]) throws Exception {
        int join_id = 0;      
        setVariable(Integer.parseInt(args[0]));
		System.out.println("My IP : " + InetAddress.getLocalHost().getHostAddress());
		System.out.println("My Port : " + portNo);	
        ExecutorService executor = Executors.newFixedThreadPool(total_client_no);
        try {
            server_socket = new ServerSocket(portNo);
        } catch(IOException e) {
            e.printStackTrace();
        }
        /* Create a client socket for each connection and pass it to a new client thread. */
        while(true) {
            try {
                Socket socket_client = server_socket.accept();
                clienthread_extd_thread client_th=new clienthread_extd_thread(socket_client, threads, chat_rooms, join_id++);
                for(int temp = 0; temp < total_client_no; temp++)//add thread to the list 
                {
                    if(threads[temp] == null) {
                        threads[temp] = client_th;
                        break;
                    }
                }
                executor.execute(client_th);
            } catch(IOException e) {
                e.printStackTrace();
                break;
            }
        }
        try {
            executor.shutdown();
            server_socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


}
