package model;

import java.util.Collections;
import java.util.List;
import service.ShowtimeService;

public class Reservation {
    private int uid;
    private int memberUid;
    private int movieUid; // 新增電影 UID
    private int theaterUid; // 新增放映廳 UID
    private String time; // 新增時間屬性
    private String seatNo; // 新增座位屬性
    private String status;
    private int numTickets; // 新增票數屬性

    public Reservation(int uid, int memberUid, int movieUid, int theaterUid, String time, String seatNo, String status, int numTickets) {
        this.uid = uid;
        this.memberUid = memberUid;
        this.movieUid = movieUid;
        this.theaterUid = theaterUid;
        this.time = time;
        this.seatNo = seatNo;
        this.status = status;
        this.numTickets = numTickets;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getMemberUid() {
        return memberUid;
    }

    public void setMemberUid(int memberUid) {
        this.memberUid = memberUid;
    }

    public int getMovieUid() {
        return movieUid;
    }

    public void setMovieUid(int movieUid) {
        this.movieUid = movieUid;
    }

    public int getTheaterUid() {
        return theaterUid;
    }

    public void setTheaterUid(int theaterUid) {
        this.theaterUid = theaterUid;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getNumTickets() {
        return numTickets;
    }

    public void setNumTickets(int numTickets) {
        this.numTickets = numTickets;
    }

    /**
     * Returns the showtime associated with this reservation.
     */
    public Showtime getShowtime() {
        return new ShowtimeService().getShowtimeById(getShowtimeUid());
    }

    /**
     * Returns the list of seat numbers for this reservation.
     */
    public List<String> getSeatNumbers() {
        return Collections.singletonList(getSeatNo());
    }

    /**
     * Returns the showtime UID for this reservation.
     */
    public int getShowtimeUid() {
        return theaterUid; // returns the underlying theaterUid repurposed
    }

    @Override
    public String toString() {
        return String.format("Reservation [ID: %d, MemberID: %d, MovieID: %d, TheaterID: %d, Time: %s, Seat: %s, Status: %s, NumTickets: %d]",
                uid, memberUid, movieUid, theaterUid, time, seatNo, status, numTickets);
    }
}