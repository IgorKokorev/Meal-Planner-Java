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
  static final List<String> CATEGORIES = Arrays.asList("breakfast", "lunch", "dinner");
  static Connection connection;
  static Statement statement;

  public static void main(String[] args) /*throws SQLException*/ {

    try {
      readDB();
    } catch (SQLException e) {
      System.out.println("Error loading DB: " + e.getMessage());
      System.out.println(Arrays.toString(e.getStackTrace()));
    }

    try {
      menu();
    } catch (SQLException e) {
      System.out.println("Error working with DB.");
      try {
        closeDB();
      } catch (SQLException ec) {
        System.out.println("Error closing DB.");
      }
    }

    try {
      closeDB();
    } catch (SQLException e) {
      System.out.println("Error closing DB.");
    }

    System.out.println("Bye!");
  }

  private static void closeDB() throws SQLException {
    statement.close();
    connection.close();
  }

  private static void readDB() throws SQLException {
    String DB_URL = "jdbc:postgresql:meals_db";
    String USER = "postgres";
    String PASS = "1111";

    connection = DriverManager.getConnection(DB_URL, USER, PASS);
    connection.setAutoCommit(true);
    statement = connection.createStatement();

    statement.executeUpdate("CREATE TABLE IF NOT EXISTS meals (" +
            "meal_id  INT           GENERATED ALWAYS AS IDENTITY," +
            "name     varchar(30)   NOT NULL," +
            "category varchar(10)   NOT NULL," +
            "PRIMARY KEY(meal_id)" +
            ")");

    statement.executeUpdate("CREATE TABLE IF NOT EXISTS ingredients (" +
            "ingredient_id  INT           GENERATED ALWAYS AS IDENTITY," +
            "meal_id        INT           NOT NULL," +
            "ingredient     VARCHAR(30)   NOT NULL," +
            "PRIMARY KEY(ingredient_id)," +
            "CONSTRAINT fk_meal" +
              "FOREIGN KEY(meal_id)" +
              "REFERENCES meals(meal_id)" +
            ");");

//    DatabaseMetaData dbm = connection.getMetaData();

    ResultSet rsMeal = statement.executeQuery("select * from meals;");
    while (rsMeal.next()) {
      String name = rsMeal.getString("name");
      System.out.println("Name: " + name);
      String category = rsMeal.getString("category");
      System.out.println("Category: " + category);
      int id = rsMeal.getInt("meal_id");
      System.out.println("ID: " + id);

      ResultSet rsIngredients = statement.executeQuery(
              "select ingredient from ingredients as i" +
              "where i.meal_id = " + id + ";");

      List<String> ingrList = new ArrayList<>();
      while (rsIngredients.next()) {
        ingrList.add(rsIngredients.getString("ingredient"));
      }

      meals.add(new Meal(category, name, ingrList));
    }

  }

  private static void menu() throws SQLException {
    while (true) {
      System.out.println("What would you like to do (add, show, exit)?");
      switch (scanner.nextLine()) {
        case "add" -> addMeal();
        case "show" -> showAllMeals();
        case "exit" -> {
          return;
        }
      }
    }
  }

  private static void showAllMeals() {
    if (meals.size() == 0) System.out.println("No meals saved. Add a meal first.");
    else meals.forEach(Meal::printMeal);
  }

  private static void addMeal() throws SQLException {

    String category;
    System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
    while (true) {
      category = scanner.nextLine();
      if (CATEGORIES.contains(category)) break;
      System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
    }

    String name;
    System.out.println("Input the meal's name:");
    while (!isCorrectName(name = scanner.nextLine()))
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

    meals.add(new Meal(category, name, Arrays.asList(ingredients)));
    ResultSet rs = statement.executeQuery(
            "INSERT INTO meals (name, category)" +
            "VALUES (\'" + name + "\', \'" + category + "\');");
    System.out.println("The meal has been added!");
  }

  private static boolean isCorrectName(String s) {

    Pattern p = Pattern.compile("[a-zA-Z ]+");
    return p.matcher(s).matches();
  }
}

