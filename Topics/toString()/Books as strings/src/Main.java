import java.util.Arrays;

class Book {

    private String title;
    private int yearOfPublishing;
    private String[] authors;

    public Book(String title, int yearOfPublishing, String[] authors) {
        this.title = title;
        this.yearOfPublishing = yearOfPublishing;
        this.authors = authors;
    }

    @Override
    public String toString() {
        String authorsStr = Arrays.toString(this.authors);
        return "title=" + this.title + "," +
                "yearOfPublishing=" + this.yearOfPublishing + "," +
                "authors=[" + (authorsStr.substring(1, authorsStr.length() - 1).replace(", ", ",")) +
                "]";
    }
}