package ui;

import service.MemberService;
import service.MovieService;
import service.ShowtimeService;
import service.ReservationService;
import util.DBUtil;
import model.Member;
import model.Movie;
import exception.AgeRestrictionException;
import exception.SeatUnavailableException;

import java.util.InputMismatchException;
import java.util.Optional;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class Main {
    private static final MemberService memberService = new MemberService();
    private static final MovieService movieService = new MovieService();
    private static final ShowtimeService showtimeService = new ShowtimeService();
    private static final ReservationService reservationService = new ReservationService();

    public static void main(String[] args) {
        // 檢查資料庫是否存在
        File dbFile = new File("cinema_booking.db");
        if (!dbFile.exists()) {
            System.out.println("Database not found. Initializing database...");
            DBUtil.initializeDatabase();
            System.out.println("Database initialized successfully.");
        } else {
            System.out.println("Database found. Skipping initialization.");
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("Welcome to the Cinema Booking System");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1 -> register(scanner);
                    case 2 -> login(scanner);
                    case 3 -> {
                        System.out.println("Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static void register(Scanner scanner) {
        while (true) {
            try {
                System.out.print("Enter email: ");
                String email = scanner.nextLine();
                System.out.print("Enter password: ");
                String password = scanner.nextLine();
                System.out.print("Enter birth date (YYYY-MM-DD): ");
                String birthDate = scanner.nextLine();

                memberService.register(email, password, birthDate);
                System.out.println("Registration successful!");
                break; // Exit loop on success
            } catch (IllegalArgumentException e) {
                System.out.println("Error during registration: " + e.getMessage());
                System.out.println("Possible reasons: Email already exists, invalid birth date format, or other input errors.");
                System.out.println("Please try again.");
            }
        }
    }

    private static void login(Scanner scanner) {
        while (true) {
            try {
                System.out.print("Enter email: ");
                String email = scanner.nextLine();
                System.out.print("Enter password: ");
                String password = scanner.nextLine();

                Member user = memberService.login(email, password);
                if (user != null) {
                    if ("admin@admin.com".equals(user.getEmail())) {
                        adminMenu(scanner);
                    } else {
                        System.out.println("Login successful!");
                        userMenu(scanner);
                    }
                    break; // Exit loop on success
                } else {
                    System.out.println("Login failed: Invalid email or password.");
                    System.out.println("Possible reasons: Incorrect email/password combination or unregistered account.");
                    System.out.println("Please try again.");
                }
            } catch (Exception e) {
                System.out.println("Error during login: " + e.getMessage());
                System.out.println("Please try again.");
            }
        }
    }

    private static void userMenu(Scanner scanner) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("User Menu");
            System.out.println("1. View Movies and Showtimes");
            System.out.println("2. Book Tickets");
            System.out.println("3. View My Reservations");
            System.out.println("4. Cancel Reservation");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1 -> movieService.displayMoviesWithShowtimes(); // 顯示電影及場次
                    case 2 -> bookTicket(scanner);
                    case 3 -> viewReservations(scanner, 1); // Assuming member ID is 1 for simplicity
                    case 4 -> cancelTicket(scanner, 1); // Assuming member ID is 1 for simplicity
                    case 5 -> {
                        System.out.println("Logged out.");
                        loggedIn = false;
                    }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static void searchMovie(Scanner scanner) {
        System.out.print("Enter movie name to search: ");
        String movieName = scanner.nextLine();

        Optional<Movie> movie = movieService.getMovieByName(movieName);
        if (movie.isPresent()) {
            System.out.println(movie.get());
            movieService.displayMoviesWithShowtimes(); // 顯示電影場次
        } else {
            System.out.println("Movie not found.");
        }
    }

    private static void bookTicket(Scanner scanner) {
        while (true) {
            try {
                System.out.print("Enter showtime ID: ");
                int showtimeId = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                System.out.println("Enter number of tickets:");
                int numTickets = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                List<String> seatNumbers = new ArrayList<>();
                for (int i = 0; i < numTickets; i++) {
                    System.out.println("Enter seat number for ticket " + (i + 1) + ":");
                    String seatNo = scanner.nextLine();
                    seatNumbers.add(seatNo);
                }

                // 呼叫訂票服務並處理回傳結果
                String result = reservationService.bookTickets(1, showtimeId, seatNumbers); // 假設會員 ID 為 1
                System.out.println(result);

                if (result.startsWith("訂票成功")) {
                    break; // 成功後退出迴圈
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input: Please enter valid data.");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static void cancelTicket(Scanner scanner, int memberUid) {
        while (true) {
            try {
                System.out.print("Enter reservation ID to cancel: ");
                int reservationId = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                boolean success = reservationService.cancelReservation(reservationId, memberUid);
                if (success) {
                    System.out.println("Ticket cancelled successfully.");
                    break; // Exit loop on success
                } else {
                    System.out.println("Cancellation failed: Unable to cancel the ticket.");
                    System.out.println("Possible reasons: Invalid reservation ID, reservation already cancelled, or cancellation window expired.");
                    System.out.println("Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input: Please enter a valid reservation ID.");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static void viewReservations(Scanner scanner, int memberUid) {
        reservationService.listReservationsByMember(memberUid).forEach(System.out::println);
    }

    private static void adminMenu(Scanner scanner) {
        boolean adminRunning = true;
        while (adminRunning) {
            System.out.println("Admin Menu");
            System.out.println("1. Reset Database to Default");
            System.out.println("2. View/Modify Reservation Status");
            System.out.println("3. Add Movie");
            System.out.println("4. Remove Movie");
            System.out.println("5. Update Showtime Time");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");
            try {
                int choice = scanner.nextInt(); scanner.nextLine();
                switch (choice) {
                    case 1 -> {
                        DBUtil.clearDatabase();
                        DBUtil.initializeDatabase();
                        System.out.println("Database has been reset to default state.");
                    }
                    case 2 -> {
                        // 查詢所有訂票並修改狀態
                        reservationService.listReservations().forEach(System.out::println);
                        System.out.print("Enter reservation ID to modify: ");
                        int rid = scanner.nextInt(); scanner.nextLine();
                        System.out.print("Enter new status (CONFIRMED/CANCELLED): ");
                        String status = scanner.nextLine();
                        boolean updated = reservationService.setReservationStatus(rid, status);
                        System.out.println(updated ? "Reservation status updated." : "Failed to update status.");
                    }
                    case 3 -> {
                        System.out.print("Enter movie name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter duration (minutes): ");
                        int duration = scanner.nextInt(); scanner.nextLine();
                        System.out.print("Enter description: ");
                        String desc = scanner.nextLine();
                        System.out.print("Enter rating (G/PG/PG-13/R/NC-17): ");
                        String rating = scanner.nextLine();
                        Movie added = movieService.addMovie(name, duration, desc, rating);
                        System.out.println(added != null ? "Movie added successfully." : "Failed to add movie.");
                    }
                    case 4 -> {
                        System.out.print("Enter movie ID to remove: ");
                        int mid = scanner.nextInt(); scanner.nextLine();
                        boolean removed = movieService.removeMovie(mid);
                        System.out.println(removed ? "Movie removed." : "Failed to remove movie.");
                    }
                    case 5 -> {
                        System.out.print("Enter showtime ID to update: ");
                        int sid = scanner.nextInt(); scanner.nextLine();
                        System.out.print("Enter new time (yyyy-MM-dd HH:mm): ");
                        String newTime = scanner.nextLine();
                        boolean ok = movieService.updateShowtimeTime(sid, newTime);
                        System.out.println(ok ? "Showtime updated." : "Failed to update showtime.");
                    }
                    case 6 -> adminRunning = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input."); scanner.nextLine();
            }
        }
    }
}