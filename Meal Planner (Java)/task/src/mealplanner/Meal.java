package mealplanner;

import java.util.Arrays;
import java.sql.*;
import java.util.List;

public class Meal {
    String category;
    String name;
    List<String> ingredients;

    public Meal(String category, String name, List<String> ingredients) {
        this.category = category;
        this.name = name;
        this.ingredients = ingredients;
    }

    public void printMeal() {
        System.out.println("\nCategory: " + category);
        System.out.println("Name: " + name);
        System.out.println("Ingredients:");
        ingredients.forEach(System.out::println);
    }
}
