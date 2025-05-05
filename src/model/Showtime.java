package model;

import service.MovieService;
import service.TheaterService;

public class Showtime {
    private int uid;
    private int movieUid;
    private int theaterUid;
    private String time;
    private int availableSeats; // 新增可用座位數

    // Constructor for reading from DB
    public Showtime(int uid, int movieUid, int theaterUid, String time, int availableSeats) {
        this.uid = uid;
        this.movieUid = movieUid;
        this.theaterUid = theaterUid;
        this.time = time;
        this.availableSeats = availableSeats;
    }

    // Constructor for creating new showtime (availableSeats will be set by DAO)
    public Showtime(int uid, int movieUid, int theaterUid, String time) {
        this(uid, movieUid, theaterUid, time, 0); // Initialize with 0, DAO will set the correct value
    }

    public int getUid() {
        return uid;
    }

    public int getMovieUid() {
        return movieUid;
    }

    public int getTheaterUid() {
        return theaterUid;
    }

    public String getTime() {
        return time;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    // Setter might not be needed if only DAO modifies it
    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public void setTime(String time) {
        this.time = time;
    }

    /**
     * Returns the Movie object associated with this showtime.
     */
    public Movie getMovie() {
        return new MovieService().getMovieById(movieUid).orElse(null);
    }

    /**
     * Returns the Theater object associated with this showtime.
     */
    public Theater getTheater() {
        return new TheaterService().getTheaterById(theaterUid).orElse(null);
    }

    /**
     * Returns the show time string.
     */
    public String getShowTime() {
        return getTime();
    }

    @Override
    public String toString() {
        return String.format("Showtime [ID: %d, MovieID: %d, TheaterID: %d, Time: %s, Available Seats: %d]",
                             uid, movieUid, theaterUid, time, availableSeats);
    }
}