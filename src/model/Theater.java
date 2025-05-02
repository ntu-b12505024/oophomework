package model;

public class Theater {
    private int uid;
    private String type; // e.g., "大廳", "小廳"
    private int totalSeats;

    public Theater(int uid, String type, int totalSeats) {
        this.uid = uid;
        this.type = type;
        this.totalSeats = totalSeats;
    }

    public int getUid() {
        return uid;
    }

    public String getType() {
        return type;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    @Override
    public String toString() {
        return "Theater [uid=" + uid + ", type=" + type + ", totalSeats=" + totalSeats + "]";
    }
}
