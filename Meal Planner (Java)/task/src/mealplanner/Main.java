package mealplanner;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.sql.*;

public class Main {
    // List of meals in DB
    static List<Meal> meals = new ArrayList<>();
    // Meals categories
    static final int NUM_CAT = 3;
    static final List<String> CATEGORIES = Arrays.asList("breakfast", "lunch", "dinner");
    // Days for planning
    static final int NUM_DAYS = 7;
    static final List<String> DAYS = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    // Meals plan
    static Meal[][] plan = new Meal[NUM_DAYS][NUM_CAT];
    static boolean isPlanReady = false;
    static Map<String, Integer> shopList = new HashMap<>();

    static Scanner scanner = new Scanner(System.in);
    // DB connection
    static WorkWithDB db = new WorkWithDB();

    public static void main(String[] args) {

        try {
            db.readDB();
        } catch (SQLException e) {
            System.out.println("Error loading DB: " + e.getMessage());
        }

        try {
            menu();
        } catch (SQLException e) {
            System.out.println("Error working with DB: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error writing file: " + e.getMessage());
        }

        System.out.println("Bye!");
    }

    // Main application menu
    private static void menu() throws SQLException, IOException {
        while (true) {
            System.out.println("\nWhat would you like to do (add, show, plan, save, exit)?");
            switch (scanner.nextLine()) {
                case "add" -> addMeal();
                case "show" -> showAllMeals();
                case "plan" -> planMeals();
                case "save" -> savePlan();
                case "exit" -> {
                    return;
                }
            }
        }
    }

    // saving plan to file
    private static void savePlan() throws IOException {
        if (!isPlanReady) {
            System.out.println("Unable to save. Plan your meals first.");
            return;
        }

        // creating list of ingredients for the plan with quantity
        createIngredientsMap();

        System.out.println("Input a filename:");
        String outFileName = scanner.nextLine();
        FileWriter fw = new FileWriter(outFileName);
        for (String ingredient : shopList.keySet()) {
            fw.write(ingredient);
            int num = shopList.get(ingredient);
            if (num > 1) fw.write(" x" + num);
            fw.write("\n");
        }
        fw.close();
        System.out.println("Saved!");
    }

    private static void createIngredientsMap() {
        for (int day = 0; day < NUM_DAYS; day++) { // every day
            for (int cat = 0; cat < NUM_CAT; cat++) { // every category
                for(String ingredient: plan[day][cat].ingredients) { // every ingredient
                    shopList.put(ingredient, shopList.getOrDefault(ingredient, 0) + 1);
                }
            }
        }
    }

    // Ask user to plan meals for every day, adds it to plan[][] and saves the plan to DB
    private static void planMeals() throws SQLException {
        // splitting all meals by categories
        ArrayList<List<Meal>> mealsByCat = new ArrayList<>();

        for (int i = 0; i < NUM_CAT; i++) {
            int finalI = i;
            mealsByCat.add(meals.stream()
                    .filter(meal -> meal.category.equals(CATEGORIES.get(finalI)))
                    .sorted()
                    .toList()
            );
        }

        for (int day = 0; day < NUM_DAYS; day++) { // For every day
            System.out.println(DAYS.get(day));

            for (int cat = 0; cat < NUM_CAT; cat ++) { // For every category

                // Printing names of meals in the category
                mealsByCat.get(cat).forEach(Meal::printName);

                // Ask to select
                System.out.println("Choose the " + CATEGORIES.get(cat) + " for " + DAYS.get(day) + " from the list above:");
                int indx;
                while (true) { // while the input is incorrect
                    String nameMeal = scanner.nextLine();
                    // getting index from list by the name of meal
                    indx = getMealIndex(nameMeal, mealsByCat.get(cat));
                    if (indx < 0) {
                        System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                    }
                    else break; // the input is correct
                }

                plan[day][cat] = mealsByCat.get(cat).get(indx); // saving selection
            }

            System.out.println("Yeah! We planned the meals for " + DAYS.get(day) + ".\n");
        }

        isPlanReady = true;

        // saving plan to DB
        db.savePlanToDB();

        // printing plan to the console
        printPlan();
    }


    // searching meal's index in a list by the name
    private static int getMealIndex(String nameMeal, List<Meal> meals) {
        for (int i = 0; i < meals.size(); i++) {
            if (meals.get(i).meal.equals(nameMeal)) return i;
        }
        return -1;
    }

    // searching meal's index in a list by the name and category
    protected static int getMealIndex(String meal, String category, List<Meal> meals) {
        for (int i = 0; i < meals.size(); i++) {
            if (meals.get(i).meal.equals(meal) && meals.get(i).category.equals(category)) return i;
        }
        return -1;
    }

    // printing weakly plan
    private static void printPlan() {
        for (int day = 0; day < NUM_DAYS; day++) {
            System.out.println("\n" + DAYS.get(day));
            for (int cat = 0; cat < NUM_CAT; cat++) {
                System.out.println(CATEGORIES.get(cat) + ": " + plan[day][cat].meal);
            }
        }
    }

    // print to the console all the meals in the DB
    private static void showAllMeals() {
        if (meals.size() == 0) System.out.println("No meals saved. Add a meal first.");
        else {
            // asking for category to print
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

        db.addMealToDB(category, meal, ingredients);

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

