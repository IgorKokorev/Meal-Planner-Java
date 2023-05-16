package mealplanner;

import java.util.List;

class Meal implements Comparable {
    String category;
    String meal;
    List<String> ingredients;

    public Meal(String category, String meal, List<String> ingredients) {
        this.category = category;
        this.meal = meal;
        this.ingredients = ingredients;
    }

    public void printMeal() {
        System.out.println();
//        System.out.println("Category: " + category);
        System.out.println("Name: " + meal);
        System.out.println("Ingredients:");
        ingredients.forEach(System.out::println);
    }

    public void printName() {
        System.out.println(this.meal);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Meal)) return false;
        return this.meal.equals(((Meal) o).meal) && this.category.equals(((Meal) o).category);
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof Meal)) return 1;
        return this.meal.compareTo(((Meal) o).meal);
    }
}
