package mealplanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.sql.*;

public class Main {
    static Scanner scanner = new Scanner(System.in);
    static List<Meal> meals = new ArrayList<>();
    static final int NUM_CAT = 3;
    static final List<String> CATEGORIES = Arrays.asList("breakfast", "lunch", "dinner");
    static final int NUM_DAYS = 7;
    static final List<String> DAYS = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    static Meal[][] plan = new Meal[NUM_DAYS][NUM_CAT];
    static String DB_URL = "jdbc:postgresql:meals_db";
    static String USER = "postgres";
    static String PASS = "1111";
    static Connection connection;
    static Statement statement;

    public static void main(String[] args) /*throws SQLException*/ {

        try {
            readDB();
        } catch (SQLException e) {
            System.out.println("Error loading DB: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            menu();
        } catch (SQLException e) {
            System.out.println("Error working with DB: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Bye!");
    }

    private static void menu() throws SQLException {
        while (true) {
            System.out.println("\nWhat would you like to do (add, show, plan, exit)?");
            switch (scanner.nextLine()) {
                case "add" -> addMeal();
                case "show" -> showAllMeals();
                case "plan" -> planMeals();
                case "exit" -> {
                    return;
                }
            }
        }
    }

    private static void planMeals() throws SQLException {
        ArrayList<List<Meal>> mealsByCat = new ArrayList<>();

        for (int i = 0; i < NUM_CAT; i++) {
            int finalI = i;
            mealsByCat.add(meals.stream()
                    .filter(meal -> meal.category.equals(CATEGORIES.get(finalI)))
                    .sorted()
                    .toList()
            );
        }

        for (int day = 0; day < NUM_DAYS; day++) {
            System.out.println(DAYS.get(day));
            for (int cat = 0; cat < NUM_CAT; cat ++) {
                for (int i = 0; i < mealsByCat.get(cat).size(); i++) {
                    System.out.println(mealsByCat.get(cat).get(i).meal);
                }
                System.out.println("Choose the " + CATEGORIES.get(cat) + " for " + DAYS.get(day) + " from the list above:");
                int indx;
                while (true) {
                    String nameMeal = scanner.nextLine();
                    indx = getMealIndex(nameMeal, mealsByCat.get(cat));
                    if (indx < 0) {
                        System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                    }
                    else break;
                }
                plan[day][cat] = mealsByCat.get(cat).get(indx);
            }
            System.out.println("Yeah! We planned the meals for " + DAYS.get(day) + ".\n");
        }

        connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(true);
        statement = connection.createStatement();

        statement.executeUpdate("DELETE FROM plan;");

        for (int day = 0; day < NUM_DAYS; day++) {
            for (int cat = 0; cat < NUM_CAT; cat++) {

                ResultSet rs = statement.executeQuery(
                        "select meal_id from meals\n" +
                                "where meal = '" + plan[day][cat].meal + "' and " +
                                "category = '" + plan[day][cat].category + "';");
                rs.next();
                int mead_id = rs.getInt("meal_id");


                statement.executeUpdate(
                        "INSERT INTO plan (day, meal_id, meal, category)\n" +
                                "VALUES ('" + DAYS.get(day) + "', '" +
                                mead_id + "', '" +
                                plan[day][cat].meal + "', '" +
                                plan[day][cat].category + "');");

            }
        }

        statement.close();
        connection.close();

        printPlan();
    }

    private static int getMealIndex(String nameMeal, List<Meal> meals) {
        for (int i = 0; i < meals.size(); i++) {
            if (meals.get(i).meal.equals(nameMeal)) return i;
        }
        return -1;
    }

    private static void printPlan() {
        for (int day = 0; day < NUM_DAYS; day++) {
            System.out.println("\n" + DAYS.get(day));
            for (int cat = 0; cat < NUM_CAT; cat++) {
                System.out.println(CATEGORIES.get(cat) + ": " + plan[day][cat].meal);
            }
        }
    }

    private static void readDB() throws SQLException {

        connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(true);
        statement = connection.createStatement();

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS meals (
                  meal_id     INT           GENERATED ALWAYS AS IDENTITY,
                  meal        varchar(30)   NOT NULL,
                  category    varchar(10)   NOT NULL,
                PRIMARY KEY(meal_id)
                );
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ingredients (
                  ingredient_id  INT           GENERATED ALWAYS AS IDENTITY,
                  meal_id        INT           NOT NULL,
                  ingredient     VARCHAR(30)   NOT NULL,
                PRIMARY KEY(ingredient_id),
                CONSTRAINT fk_meal
                  FOREIGN KEY(meal_id)
                  REFERENCES meals(meal_id)
                );
                """);

      statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS plan (
                  day       varchar(10)   NOT NULL,
                  meal_id   INT           NOT NULL,
                  meal      varchar(30)   NOT NULL,
                  category  varchar(10)   NOT NULL,
                  CONSTRAINT fk_meal
                  FOREIGN KEY(meal_id)
                  REFERENCES meals(meal_id)
                );
                """);

        ResultSet rsMeal = statement.executeQuery("select * from meals;");

        while (rsMeal.next()) {
            String meal = rsMeal.getString("meal");
            String category = rsMeal.getString("category");
            int id = rsMeal.getInt("meal_id");

            Statement stIngredients = connection.createStatement();
            ResultSet rsIngredients = stIngredients.executeQuery(
                    "select ingredient from ingredients\n" +
                            "where meal_id = " + id + ";");

            List<String> ingrList = new ArrayList<>();
            while (rsIngredients.next()) {
                ingrList.add(rsIngredients.getString("ingredient"));
            }

            stIngredients.close();

            meals.add(new Meal(category, meal, ingrList));
        }



        statement.close();
        connection.close();
    }

    private static void showAllMeals() {
        if (meals.size() == 0) System.out.println("No meals saved. Add a meal first.");
        else {
            System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
            String category = getCategory();

            List<Meal> selectedMeals = meals.stream()
                    .filter(m -> m.category.equals(category))
                    .toList();

            if (selectedMeals.size() == 0) {
                System.out.println("No meals found.");
            } else {
                System.out.println("Category: " + category);
                selectedMeals.forEach(Meal::printMeal);
            }
        }
    }

    private static void addMeal() throws SQLException {

        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        String category = getCategory();

        String meal;
        System.out.println("Input the meal's name:");
        while (!isCorrectName(meal = scanner.nextLine()))
            System.out.println("Wrong format. Use letters only!");

        System.out.println("Input the ingredients:");
        String[] ingredients;
        while (true) {
            ingredients = scanner.nextLine().split(", *", -1);
            if (ingredients.length == 0 ||
                    Arrays.stream(ingredients)
                            .map(Main::isCorrectName)
                            .reduce((b1, b2) -> b1 && b2).get()) {
                break;
            }
            System.out.println("Wrong format. Use letters only!");
        }

        Meal newMeal = new Meal(category, meal, Arrays.asList(ingredients));
        if (meals.contains(newMeal)) {
            System.out.println("DB already contains meal " + meal + " in category " + category + ".");
            return;
        }

        meals.add(newMeal);

        connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(true);
        statement = connection.createStatement();

        statement.executeUpdate(
                "INSERT INTO meals (meal, category)\n" +
                        "VALUES ('" + meal + "', '" + category + "');");
        ResultSet rs = statement.executeQuery(
                "select meal_id from meals\n" +
                        "where meal = '" + meal + "' and " +
                        "category = '" + category + "';");
        rs.next();
        int id = rs.getInt("meal_id");
        for (String ingr : ingredients) {
            statement.executeUpdate(
                    "INSERT INTO ingredients (meal_id, ingredient)\n" +
                            "VALUES ('" + id + "', '" + ingr + "');");
        }

        statement.close();
        connection.close();

        System.out.println("The meal has been added!");
    }

    private static String getCategory() {
        String category;
        while (true) {
            category = scanner.nextLine();
            if (CATEGORIES.contains(category)) break;
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
        }
        return category;
    }

    private static boolean isCorrectName(String s) {

        Pattern p = Pattern.compile("[a-zA-Z ]+");
        return p.matcher(s).matches();
    }
}

