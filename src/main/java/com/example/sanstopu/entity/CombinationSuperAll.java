package com.example.sanstopu.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "combinationSuperAll")
public class CombinationSuperAll {
    @Id
    @Indexed
    private String numbers; // JSON field for storing the combination

    private int count; // Count of how many times this set appears

    public CombinationSuperAll() {}

    public CombinationSuperAll(String numbers, int count) {
        this.numbers = numbers;
        this.count = count;
    }



    public String getNumbers() {
        return numbers;
    }

    public void setNumbers(String numbers) {
        this.numbers = numbers;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
