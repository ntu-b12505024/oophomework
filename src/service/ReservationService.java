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
import java.util.stream.Collectors;
import java.util.ArrayList;

public class ReservationService {
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final MovieDAO movieDAO = new MovieDAO();

    public void bookTicket(int memberUid, int showtimeUid, String seatNo, int numTickets) throws AgeRestrictionException, SeatUnavailableException {
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

        // 檢查座位是否已被佔用
        List<Reservation> reservations = reservationDAO.getReservationsByShowtimeAndSeat(showtimeUid, seatNo);
        if (!reservations.isEmpty()) {
            throw new SeatUnavailableException("該座位已被佔用，請選擇其他座位");
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

    public String bookTickets(int memberUid, int showtimeUid, List<String> seatNumbers) {
        try {
            // 檢查會員是否存在
            Member member = memberDAO.getMemberByUid(memberUid);
            if (member == null) {
                return "會員不存在";
            }

            // 檢查電影場次是否存在
            Showtime showtime = showtimeDAO.getShowtimeById(showtimeUid);
            if (showtime == null) {
                return "電影場次不存在";
            }

            // 檢查電影分級與會員年齡
            Movie movie = movieDAO.getMovieById(showtime.getMovieUid());
            if (movie == null) {
                return "電影不存在";
            }
            if (member.getAge() < movie.getMinimumAge()) {
                return "會員年齡不符合電影分級要求";
            }

            // 檢查座位是否足夠
            if (seatNumbers.size() > showtime.getAvailableSeats()) {
                return "座位不足";
            }

            // 檢查每個座位是否已被佔用
            for (String seatNo : seatNumbers) {
                List<Reservation> reservations = reservationDAO.getReservationsByShowtimeAndSeat(showtimeUid, seatNo);
                if (!reservations.isEmpty()) {
                    return "座位 " + seatNo + " 已被佔用，請選擇其他座位";
                }
            }

            // 減少場次的可用座位數
            boolean seatsAvailable = showtimeDAO.decreaseAvailableSeats(showtimeUid, seatNumbers.size());
            if (!seatsAvailable) {
                return "座位不足";
            }

            // 新增每個座位的訂票紀錄
            StringBuilder successMessage = new StringBuilder("訂票成功，訂票編號: ");
            for (String seatNo : seatNumbers) {
                Reservation reservation = new Reservation(0, memberUid, showtime.getMovieUid(), showtime.getTheaterUid(), showtime.getTime(), seatNo, "CONFIRMED", 1);
                int reservationId = reservationDAO.addReservation(reservation);
                if (reservationId == -1) {
                    return "座位 " + seatNo + " 訂票失敗，請稍後再試";
                }
                successMessage.append(reservationId).append(" ");
            }

            return successMessage.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "訂票過程中發生錯誤";
        }
    }

    public List<Reservation> listReservations() {
        return reservationDAO.getAllReservations();
    }

    public List<Reservation> listReservationsByMember(int memberUid) {
        return reservationDAO.getReservationsByMemberId(memberUid);
    }

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
            return false;
        }

        // 4. Update reservation status to CANCELLED
        boolean statusUpdated = reservationDAO.updateReservationStatus(reservationId, "CANCELLED");

        if (!statusUpdated) {
            System.err.println("Cancellation failed: Could not update reservation status for ID " + reservationId + ".");
            return false;
        }

        // 5. Increase available seats
        boolean seatsIncreased = showtimeDAO.increaseAvailableSeats(reservation.getShowtimeUid(), reservation.getNumTickets());
        if (!seatsIncreased) {
            System.err.println("CRITICAL ERROR: Reservation ID " + reservationId + " cancelled, but failed to increase available seats for showtime ID " + reservation.getShowtimeUid() + ". Manual correction needed.");
        }

        System.out.println("Reservation ID " + reservationId + " cancelled successfully.");
        return true;
    }

    public boolean setReservationStatus(int reservationId, String status) {
        return reservationDAO.updateReservationStatus(reservationId, status);
    }

    /**
     * Retrieves seat numbers already booked for a specific showtime.
     */
    public List<String> getBookedSeatsForShowtime(int showtimeUid) {
        // 使用新的 DAO 方法取得已確認的訂票
        return reservationDAO.getReservationsByShowtimeId(showtimeUid)
            .stream()
            .map(Reservation::getSeatNo)
            .collect(Collectors.toList());
    }
}