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
            case "G" -> 0;    // 無年齡限制
            case "PG" -> 7;   // 7 歲以上（6 歲以下包含）才能訂票
            case "PG-13" -> 13; // 13 歲以上
            case "R" -> 18;   // 18 歲以上
            case "NC-17" -> 18; // 18 歲以上
            default -> 0;      // 預設無限制
        };
    }

    @Override
    public String toString() {
        return String.format("Movie: %s (%d mins) - %s [Rating: %s, Min Age: %d]",
                             name, duration, description, rating, getMinimumAge());
    }
}