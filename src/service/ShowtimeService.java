package service;

import dao.ShowtimeDAO;
import model.Showtime;

import java.util.List;

public class ShowtimeService {
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    public void addShowtime(int movieUid, int theaterUid, String time) {
        Showtime showtime = new Showtime(0, movieUid, theaterUid, time);
        showtimeDAO.addShowtime(showtime);
    }

    public List<Showtime> listShowtimes() {
        return showtimeDAO.getAllShowtimes();
    }

    public List<Showtime> getShowtimesByMovieId(int movieId) {
        return showtimeDAO.getShowtimesByMovieId(movieId);
    }

    /**
     * Retrieves a showtime by its unique ID.
     * @param showtimeId The UID of the showtime.
     * @return The Showtime object if found, otherwise null.
     */
    public Showtime getShowtimeById(int showtimeId) {
        return showtimeDAO.getShowtimeById(showtimeId);
    }
}