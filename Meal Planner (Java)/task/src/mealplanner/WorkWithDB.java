package mealplanner;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static mealplanner.Main.*;

public class WorkWithDB {
    private final String DB_URL = "jdbc:postgresql:meals_db";
    private final String USER = "postgres";
    private final String PASS = "1111";
    private Connection connection;
    private Statement statement;

        // reading data from DB
    protected void readDB() throws SQLException {

        connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(true);
        statement = connection.createStatement();

        createDBTablesIfNeeded();
        readAllMealsFromDB();
        readPlanFromDB();

        statement.close();
        connection.close();
    }

    private void readPlanFromDB() throws SQLException {
        // reading the plan from DB
        ResultSet rsMeal = statement.executeQuery("select * from plan;");
        while (rsMeal.next()) {
            String day = rsMeal.getString("day");
            String meal = rsMeal.getString("meal");
            String category = rsMeal.getString("category");
            int numDay = DAYS.indexOf(day);
            int numCat = CATEGORIES.indexOf(category);
            int index = getMealIndex(meal, category, meals);
            if (index >= 0) {
                plan[numDay][numCat] = meals.get(index);
                isPlanReady = true;
            }
        }
    }

    private void readAllMealsFromDB() throws SQLException {
        // all the meals in DB
        ResultSet rsMeal = statement.executeQuery("select * from meals;");

        while (rsMeal.next()) {
            String meal = rsMeal.getString("meal");
            String category = rsMeal.getString("category");
            int id = rsMeal.getInt("meal_id");

            // for every meal getting  the corresponding ingredients
            Statement stIngredients = connection.createStatement();
            ResultSet rsIngredients = stIngredients.executeQuery(
                    "select ingredient from ingredients\n" +
                            "where meal_id = " + id + ";");

            List<String> ingrList = new ArrayList<>();
            while (rsIngredients.next()) {
                ingrList.add(rsIngredients.getString("ingredient"));
            }

            stIngredients.close();

            // adding the meal with ingredients to the list
            meals.add(new Meal(category, meal, ingrList));
        }
    }

    private void createDBTablesIfNeeded() throws SQLException {
        // creating table if not exists
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS meals (
                  meal_id     INT           GENERATED ALWAYS AS IDENTITY,
                  meal        varchar(30)   NOT NULL,
                  category    varchar(10)   NOT NULL,
                PRIMARY KEY(meal_id)
                );
                """);

        // creating table if not exists
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

        // creating table if not exists
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
    }

    void addMealToDB(String category, String meal, String[] ingredients) throws SQLException {
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
    }

    // Saving weakly meals plan to DB
    protected void savePlanToDB() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(true);
        statement = connection.createStatement();

        // deleting old plan
        statement.executeUpdate("DELETE FROM plan;");

        for (int day = 0; day < NUM_DAYS; day++) {
            for (int cat = 0; cat < NUM_CAT; cat++) {

                // getting meal_id from DB
                ResultSet rs = statement.executeQuery(
                        "select meal_id from meals\n" +
                                "where meal = '" + plan[day][cat].meal + "' and " +
                                "category = '" + plan[day][cat].category + "';");
                rs.next();
                int mead_id = rs.getInt("meal_id");

                // saving the row
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
    }


}

