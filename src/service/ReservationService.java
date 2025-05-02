package service;

import dao.ReservationDAO;
import dao.MemberDAO;
import dao.ShowtimeDAO;
import dao.MovieDAO;
import model.Reservation;
import exception.AgeRestrictionException;
import exception.SeatUnavailableException;
import model.Movie;
import model.Showtime;
import model.Member;

import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Duration;

public class ReservationService {
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private static final DateTimeFormatter SHOWTIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final long CANCELLATION_WINDOW_MINUTES = 30;

    public void bookTicket(int memberUid, int showtimeUid, String seatNo, int numTickets, String ticketType) throws AgeRestrictionException, SeatUnavailableException {
        // 檢查會員是否存在
        Member member = memberDAO.getMemberByUid(memberUid);
        if (member == null) {
            throw new IllegalArgumentException("會員不存在");
        }

        // 檢查電影場次是否存在
        Showtime showtime = showtimeDAO.getShowtimeById(showtimeUid);
        if (showtime == null) {
            throw new IllegalArgumentException("電影場次不存在");
        }

        // 檢查電影分級與會員年齡
        Movie movie = movieDAO.getMovieById(showtime.getMovieUid());
        if (movie == null) {
            throw new IllegalArgumentException("電影不存在");
        }
        if (member.getAge() < movie.getMinimumAge()) {
            throw new AgeRestrictionException("會員年齡不符合電影分級要求");
        }

        // 檢查座位是否足夠
        boolean seatsAvailable = showtimeDAO.decreaseAvailableSeats(showtimeUid, numTickets);
        if (!seatsAvailable) {
            throw new SeatUnavailableException("座位不足");
        }

        // 新增訂票紀錄
        Reservation reservation = new Reservation(0, memberUid, movie.getUid(), showtime.getTheaterUid(), showtime.getTime(), seatNo, "CONFIRMED", numTickets);
        int reservationId = reservationDAO.addReservation(reservation);
        if (reservationId == -1) {
            throw new RuntimeException("訂票失敗");
        }

        System.out.println("訂票成功，訂票編號: " + reservationId);
    }

    public List<Reservation> listReservations() {
        return reservationDAO.getAllReservations();
    }

    public List<Reservation> listReservationsByMember(int memberUid) {
        return reservationDAO.getReservationsByMemberId(memberUid);
    }

    /**
     * Cancels a reservation if it's within the allowed time window (30 mins before showtime).
     *
     * @param reservationId The ID of the reservation to cancel.
     * @param memberUid The ID of the member attempting the cancellation (for verification).
     * @return true if the cancellation was successful, false otherwise.
     */
    public boolean cancelReservation(int reservationId, int memberUid) {
        // 1. Get Reservation by ID
        Reservation reservation = reservationDAO.getReservationById(reservationId);

        // 2. Check if reservation exists and belongs to the member
        if (reservation == null) {
            System.err.println("Cancellation failed: Reservation with ID " + reservationId + " not found.");
            return false;
        }
        if (reservation.getMemberUid() != memberUid) {
             System.err.println("Cancellation failed: Reservation ID " + reservationId + " does not belong to member ID " + memberUid + ".");
            return false;
        }

        // 3. Check if status is already CANCELLED
        if ("CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
            System.out.println("Reservation ID " + reservationId + " is already cancelled.");
            return true; // Considered successful as the state is already achieved
        }
        if (!"CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
             System.err.println("Cancellation failed: Reservation ID " + reservationId + " has an unexpected status: " + reservation.getStatus());
            return false;
        }

        // 4. Get Showtime from reservation
        Showtime showtime = showtimeDAO.getShowtimeById(reservation.getShowtimeUid());
        if (showtime == null) {
            // Data integrity issue, but cancellation can't proceed without showtime info
            System.err.println("Cancellation failed: Could not find showtime details for reservation ID " + reservationId + ".");
            return false;
        }

        // 5. Parse Showtime time and check cancellation window
        LocalDateTime showtimeDateTime;
        try {
            showtimeDateTime = LocalDateTime.parse(showtime.getTime(), SHOWTIME_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Cancellation failed: Could not parse showtime time string: " + showtime.getTime());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        Duration timeUntilShow = Duration.between(now, showtimeDateTime);

        if (timeUntilShow.toMinutes() < CANCELLATION_WINDOW_MINUTES) {
            System.err.println("Cancellation failed: Cannot cancel reservation ID " + reservationId + ". Showtime is less than " + CANCELLATION_WINDOW_MINUTES + " minutes away.");
            return false;
        }

        // 6. Update reservation status to CANCELLED
        boolean statusUpdated = reservationDAO.updateReservationStatus(reservationId, "CANCELLED");

        if (!statusUpdated) {
            System.err.println("Cancellation failed: Could not update reservation status for ID " + reservationId + ".");
            return false;
        }

        // 7. Increase available seats
        boolean seatsIncreased = showtimeDAO.increaseAvailableSeats(reservation.getShowtimeUid(), reservation.getNumTickets());
        if (!seatsIncreased) {
            // This is problematic - status is CANCELLED but seats weren't returned.
            // Log this error. Manual intervention might be needed.
            System.err.println("CRITICAL ERROR: Reservation ID " + reservationId + " cancelled, but failed to increase available seats for showtime ID " + reservation.getShowtimeUid() + ". Manual correction needed.");
            // Even though seats failed, the cancellation itself (status update) succeeded from user perspective.
        }

        System.out.println("Reservation ID " + reservationId + " cancelled successfully.");
        return true;
    }

    public boolean setReservationStatus(int reservationId, String status) {
        return reservationDAO.updateReservationStatus(reservationId, status);
    }
}