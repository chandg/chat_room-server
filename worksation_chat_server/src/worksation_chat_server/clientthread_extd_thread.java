package worksation_chat_server;

import worksation_chat_server.chat_room_server;
import worksation_chat_server.clienthread_extd_thread;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.TreeSet;


class clienthread_extd_thread extends Thread {

    private BufferedReader input_stream = null;
    private PrintWriter output_stream = null;
    private Socket socket_client = null;
    private clienthread_extd_thread[] threads;
    private int maxClientsCount;
    private long join_id = -1;
    private TreeSet<chat_rooms> chatRooms; //all rooms set
    private TreeSet<chat_rooms> joinedRoom = new TreeSet<chat_rooms>(); //client's current thread joined rooms
    public clienthread_extd_thread(Socket socket_client, clienthread_extd_thread[] threads, TreeSet<chat_rooms> chatRooms, int join_id) {
        this.socket_client = socket_client;
        this.threads = threads;
        this.chatRooms = chatRooms;
        this.maxClientsCount = threads.length;
        this.join_id = join_id;
    }
   //chat_room_server a=new chat_room_server();
    int port = chat_room_server.portNo;
    
    
    
 	/*MESSAGE BROADCAST*/
     synchronized private void broadcastToOthersInTheSameRoom(int room_Ref_0, String msg){
         for(clienthread_extd_thread c: this.threads) {
             if(c != null) {
                 for(chat_rooms room : c.joinedRoom) {
                     if(room.get_id() == (room_Ref_0)) {
                         c.output_stream.println(msg);
                         c.output_stream.flush();
                     }
                 }
             }
         }
     }

     /*get room reff ID.*/
     private int getRoomRef(String roomName) {
         if(chatRooms.size() == 0) { 
             chatRooms.add(new chat_rooms(0, roomName));
             return 0;
         }
         for(chat_rooms room : chatRooms) { 
             if(room.getName().equals(roomName)) {
                 return room.get_id();
             }
         }
         int id = chatRooms.size();
         chatRooms.add(new chat_rooms(id, roomName));
         return id;
     }
     
     
     synchronized private void remove_room_joined(int roomRef) {
         for(Iterator<chat_rooms> iterator = this.joinedRoom.iterator(); iterator.hasNext(); ) {
             chat_rooms room = iterator.next();
             if(room.get_id() == roomRef) {
                 iterator.remove();
             }
         }
     }

     synchronized private boolean isAlreadyIn(int roomRef) {
         for(chat_rooms room : this.joinedRoom) {
             if(room.get_id() == (roomRef)) {
                 return true;
             }
         }
         return false;
     }
     synchronized private void Add_room_joined(chat_rooms room) {joinedRoom.add(room); }

     
    
    public void run() {
        clienthread_extd_thread[] threads = this.threads;
        try {
            /* set up in/out streams */
            input_stream = new BufferedReader(new InputStreamReader(socket_client.getInputStream()));
            output_stream = new PrintWriter(socket_client.getOutputStream(), true);
            String line_data;        
            while((line_data = input_stream.readLine()) != null) {
                System.out.println(" receive msg:" + line_data); //debug

                
                
                /* iNITIAL TEST FROM SERVER */
                if(line_data.trim().indexOf("BASE_TEST") > 0) {
                    output_stream.println(line_data + "\n" + "IP:" + InetAddress.getLocalHost().getHostAddress() + "\n" + "Port: " + port + "\nStudentID: 17318921");
                    output_stream.flush();
                    continue;
                }
                
                
                
                /*TERMINATE COMMAND*/
                if(line_data.startsWith("KILL_SERV")) {
                    for(int temp = 0; temp < maxClientsCount; temp++) {
                        if(threads[temp]==this){
                            threads[temp].socket_client.close();
                            threads[temp].output_stream.close();
                            threads[temp].input_stream.close();
                            threads[temp] = null;
                        }
                    }
                    break;
                }
                
                                
                /* CHATROOM JOIN INSTRUCTION CHECK */
                else if(line_data.startsWith("JOIN_CHATROOM:")) {         
                    String roomName = line_data.split(":")[1].trim();
                    int room_Ref_0 = getRoomRef(roomName);
                    input_stream.readLine(); 
                    input_stream.readLine(); 
                    String clientName = input_stream.readLine().trim().split(":")[1].trim();                    
                    if(isAlreadyIn(room_Ref_0)) {
                        output_stream.println("ERROR_CODE:00");
                        output_stream.println("ERROR_DESCRIPTION: already joined this room.\n");
                        output_stream.flush();
                        continue;
                    }
                    
                    Add_room_joined(new chat_rooms(room_Ref_0, roomName));
                    /* oPTPUT FROM sERVER */
                    output_stream.println("JOINED_CHATROOM: " + roomName);
                    output_stream.println("SERVER_IP: " + InetAddress.getLocalHost().getHostAddress());
                    output_stream.println("PORT: " + port);
                    output_stream.println("ROOM_REF: " + room_Ref_0);
                    output_stream.println("JOIN_ID: " + join_id);
                    output_stream.flush();
                    /* broadcast it to all other clients in this room. */
                    String msg = "CHAT:" + room_Ref_0+"\nCLIENT_NAME:"+clientName+"\nMESSAGE:"+clientName + " has joined this chatroom.\n";
                    broadcastToOthersInTheSameRoom(room_Ref_0,msg);
                    continue;
                }
                
                
                /* MESSAGE "CHAT:" */
                else if(line_data.startsWith("CHAT:")) {
                    int room_Ref_0 = Integer.parseInt(line_data.substring(5).trim());
                    input_stream.readLine(); 
                    String clientName = input_stream.readLine().substring(12).trim();
                    String message = input_stream.readLine().substring(8).trim();
                    input_stream.readLine();
                    String msg = "CHAT:" + room_Ref_0+"\nCLIENT_NAME:" + clientName+"\nMESSAGE:" + message + "\n";
                    broadcastToOthersInTheSameRoom(room_Ref_0,msg);
                }
                
               
                
                /* REQUEST  "DISCONNECT:" */             
                else if(line_data.startsWith("DISCONNECT:")) {
                    /* read content from socket. */
                    input_stream.readLine();
                    String clientName = input_stream.readLine().substring(12).trim();
                    
                    synchronized(this) {
                        for(chat_rooms room0 : this.joinedRoom) {
                            String msg = "CHAT:" + room0.get_id()+"\nCLIENT_NAME:" + clientName+"\nMESSAGE:" + clientName + " has left this chatroom.\n";
                            broadcastToOthersInTheSameRoom(room0.get_id(),msg);
                        }
                        
                        this.joinedRoom = new TreeSet<chat_rooms>();
                    }
                    continue;
                }
                
                
                
                
                
                /* REMOVE FROM CHAT ROOM WITH INSTRIC */
                else if(line_data.startsWith("LEAVE_CHATROOM:")) {                
                    int room_Ref_0 = Integer.parseInt(line_data.substring(15).trim());
                    int join_id = Integer.parseInt(input_stream.readLine().substring(8).trim());
                    String clientName = input_stream.readLine().substring(12).trim();                   
                    output_stream.println("LEFT_CHATROOM:" + room_Ref_0);
                    output_stream.println("JOIN_ID:" + join_id);                    
                    String msg = "CHAT:" + room_Ref_0+"\nCLIENT_NAME:" + clientName+"\nMESSAGE:" + clientName + " has left this chatroom.\n";
                    broadcastToOthersInTheSameRoom(room_Ref_0,msg);                   
                    if(isAlreadyIn(room_Ref_0)) {
                        remove_room_joined(room_Ref_0);
                    } else {                        
                        output_stream.println("ERROR_CODE:00");
                        output_stream.println("ERROR_DESCRIPTION: You never joined in this room\n");
                        output_stream.flush();
                    }
                    continue;
                }
          
                
                else 
                	{System.out.println("Data format wrong");}
                
                
                
                
            }
            output_stream.flush();
        } catch(UnknownHostException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            /* close connections */
            try {
                if(socket_client != null)
                    socket_client.close();
                if(input_stream != null)
                    input_stream.close();
                if(output_stream != null)
                    output_stream.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
    
 
   

    
}



