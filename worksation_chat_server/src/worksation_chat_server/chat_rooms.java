package worksation_chat_server;



class chat_rooms implements Comparable {
    
    private String name;
    private int id;
    public int get_id() {return id; }
    public String getName() {return name; }
    public chat_rooms(int id, String name) {
        this.id = id;
        this.name = name;
    }
    @Override
    public int compareTo(Object o) {return this.get_id() - ((chat_rooms) o).get_id();}
}
