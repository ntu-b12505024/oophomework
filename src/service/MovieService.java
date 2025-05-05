package service;

import dao.MovieDAO;
import dao.ShowtimeDAO; // Import ShowtimeDAO
import dao.TheaterDAO; // Import TheaterDAO
import model.Movie;
import model.Showtime;
import model.Theater; // Import Theater

import java.util.List;
import java.util.Optional;

public class MovieService {
    private final MovieDAO movieDAO = new MovieDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO(); // Add ShowtimeDAO
    private final TheaterDAO theaterDAO = new TheaterDAO(); // Add TheaterDAO

    /**
     * Adds a new movie.
     * @return The newly added Movie object with its generated UID, or null if adding failed (e.g., duplicate name).
     */
    public Movie addMovie(String name, int duration, String description, String rating) {
        if (movieDAO.existsByName(name)) {
            System.err.println("Error adding movie: Movie with name '" + name + "' already exists.");
            return null;
        }
        Movie movie = new Movie(0, name, duration, description, rating);
        // Modify MovieDAO.addMovie to return the generated ID or the full object
        int generatedId = movieDAO.addMovie(movie); // Assuming addMovie now returns ID
        if (generatedId > 0) {
             // Re-fetch the movie to get the complete object with the ID
             return movieDAO.getMovieById(generatedId);
        } else {
            System.err.println("Failed to add movie '" + name + "' to the database.");
            return null;
        }
    }

    public List<Movie> listMovies() {
        return movieDAO.getAllMovies();
    }

    /**
     * Returns all movies.
     */
    public List<Movie> getAllMovies() {
        return listMovies();
    }

    /**
     * Gets a movie by its ID.
     * @param movieId The ID of the movie.
     * @return An Optional containing the Movie if found, otherwise empty.
     */
    public Optional<Movie> getMovieById(int movieId) {
        return Optional.ofNullable(movieDAO.getMovieById(movieId));
    }

    /**
     * Updates an existing movie's details.
     * @param movieId The ID of the movie to update.
     * @param name New name.
     * @param duration New duration.
     * @param description New description.
     * @param rating New rating.
     * @return true if the update was successful, false otherwise (e.g., movie not found).
     */
    public boolean updateMovie(int movieId, String name, int duration, String description, String rating) {
        // Check if movie exists
        if (movieDAO.getMovieById(movieId) == null) {
            System.err.println("Error updating movie: Movie with ID " + movieId + " not found.");
            return false;
        }
        // Optional: Check if the new name conflicts with another existing movie (excluding itself)
        Movie existingWithSameName = movieDAO.getMovieByName(name); // Assuming getMovieByName exists
        if (existingWithSameName != null && existingWithSameName.getUid() != movieId) {
             System.err.println("Error updating movie: Another movie with name '" + name + "' already exists.");
            return false;
        }

        Movie movieToUpdate = new Movie(movieId, name, duration, description, rating);
        boolean updated = movieDAO.updateMovie(movieToUpdate); // Assuming updateMovie returns boolean
        if (!updated) {
             System.err.println("Failed to update movie with ID " + movieId + " in the database.");
        }
        return updated;
    }

    /**
     * Removes a movie and its associated showtimes and reservations (due to ON DELETE CASCADE).
     * @param movieId The ID of the movie to remove.
     * @return true if the deletion was successful, false otherwise (e.g., movie not found).
     */
    public boolean removeMovie(int movieId) {
        // Check if movie exists before attempting deletion
        if (movieDAO.getMovieById(movieId) == null) {
            System.err.println("Error removing movie: Movie with ID " + movieId + " not found.");
            return false;
        }

        boolean deleted = movieDAO.deleteMovie(movieId); // Assuming deleteMovie returns boolean
        if (deleted) {
            System.out.println("Movie with ID " + movieId + " and its associated showtimes/reservations removed successfully.");
        } else {
             System.err.println("Failed to remove movie with ID " + movieId + " from the database.");
        }
        return deleted;
    }

    /**
     * Displays all movies along with their scheduled showtimes.
     */
    public void displayMoviesWithShowtimes() {
        List<Movie> movies = movieDAO.getAllMovies();

        if (movies.isEmpty()) {
            System.out.println("No movies found in the system.");
            return;
        }

        System.out.println("\n=== Movies and Showtimes ===");
        for (Movie movie : movies) {
            System.out.printf("[%d] %s (%d min) - Rating: %s\n",
                              movie.getUid(), movie.getName(), movie.getDuration(),
                              movie.getRating());
            System.out.println("  Description: " + movie.getDescription());

            List<Showtime> showtimes = showtimeDAO.getShowtimesByMovieId(movie.getUid());
            if (showtimes.isEmpty()) {
                System.out.println("  No scheduled showtimes.");
            } else {
                System.out.println("  Showtimes:");
                for (Showtime showtime : showtimes) {
                    System.out.printf("    - ID: %d | Time: %s | Available Seats: %d\n",
                                      showtime.getUid(), showtime.getTime(),
                                      showtime.getAvailableSeats());
                }
            }
            System.out.println("--------------------------------------------------");
        }
    }

    public Optional<Movie> getMovieByName(String name) {
        return Optional.ofNullable(movieDAO.getMovieByName(name));
    }

    public boolean updateShowtimeTime(int showtimeId, String newTime) {
        Showtime showtime = showtimeDAO.getShowtimeById(showtimeId);
        if (showtime == null) {
            System.err.println("Showtime not found.");
            return false;
        }

        // 檢查時間衝突
        List<Showtime> theaterShowtimes = showtimeDAO.getShowtimesByTheaterId(showtime.getTheaterUid());
        for (Showtime existingShowtime : theaterShowtimes) {
            if (existingShowtime.getUid() != showtimeId && existingShowtime.getTime().equals(newTime)) {
                System.err.println("Time conflict detected with another showtime.");
                return false;
            }
        }

        // 更新場次時間
        showtime.setTime(newTime);
        return showtimeDAO.updateShowtime(showtime);
    }

    public List<Movie> getCurrentlyShowingMovies() {
        List<Movie> movies = movieDAO.getAllMovies();
        for (Movie movie : movies) {
            List<Showtime> showtimes = showtimeDAO.getShowtimesByMovieId(movie.getUid());
            System.out.println("Movie: " + movie.getName());
            for (Showtime showtime : showtimes) {
                System.out.printf("  Showtime: %s | Theater: %d | Available Seats: %d\n",
                                  showtime.getTime(), showtime.getTheaterUid(), showtime.getAvailableSeats());
            }
        }
        return movies;
    }
}