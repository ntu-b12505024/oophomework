package model;

public class Movie {
    private int uid;
    private String name;
    private int duration;
    private String description;
    private String rating;

    public Movie(int uid, String name, int duration, String description, String rating) {
        this.uid = uid;
        this.name = name;
        this.duration = duration;
        this.description = description;
        this.rating = rating;
    }

    public int getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public String getDescription() {
        return description;
    }

    public String getRating() {
        return rating;
    }

    /**
     * Gets the minimum recommended age based on the movie rating.
     * This is a simplified mapping.
     * Returns 0 if the rating implies no age restriction or is unknown.
     */
    public int getMinimumAge() {
        return switch (this.rating.toUpperCase()) {
            case "G" -> 0; // General Audiences
            case "PG" -> 0; // Parental Guidance Suggested (Technically no restriction, but let's treat as 0 for simplicity)
            case "PG-13" -> 13; // Parents Strongly Cautioned
            case "R" -> 17; // Restricted
            case "NC-17" -> 18; // Adults Only
            default -> 0; // Default to no restriction for unknown ratings
        };
    }

    @Override
    public String toString() {
        return String.format("Movie: %s (%d mins) - %s [Rating: %s, Min Age: %d]",
                             name, duration, description, rating, getMinimumAge());
    }
}