package mealplanner;

import java.util.List;

public class Meal {
    String category;
    String meal;
    List<String> ingredients;

    public Meal(String category, String meal, List<String> ingredients) {
        this.category = category;
        this.meal = meal;
        this.ingredients = ingredients;
    }

    public void printMeal() {
        System.out.println("\nCategory: " + category);
        System.out.println("Name: " + meal);
        System.out.println("Ingredients:");
        ingredients.forEach(System.out::println);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Meal)) return false;
        return this.meal.equals(((Meal) o).meal) && this.category.equals(((Meal) o).category);
    }
}
