package com.example.sanstopu.controller;


import com.example.sanstopu.service.SansTopuCombinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/sansTopu")
public class SansTopuController {

    @Autowired
    private SansTopuCombinationService combinationService;

    @PostMapping("/insertAllCombinations")
    public String generateAllSixNumberCombinations() {
        try {
            combinationService.generateAndSaveAllCombinations();
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }

        return "All 6-number combinations for numbers 1-60 have been generated and saved to the database.";
    }


    @GetMapping("/combinationAll/count")
    public long getCombinationSuperAllCount() {
        try {
            return combinationService.getCombinationAllCount();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while fetching the count", e);
        }
    }

    @PostMapping("/removeCombinations")
    public String removeCombinations(@RequestBody List<List<Integer>> inputSets) {
        if (inputSets.isEmpty()) {
            return "Input set list cannot be empty.";
        }

        try {
            return combinationService.removeCombinations(inputSets);
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }
    }

    @PostMapping("/generate")
    public String generateCombinations(@RequestBody List<List<Integer>> drawnNumbers) {
        if (drawnNumbers.isEmpty()) {
            return "You must provide at least one set of numbers.";
        }

        try {
            for (List<Integer> numbers : drawnNumbers) {
                if (numbers.size() != 5) {
                    return "Each set must contain exactly 5 numbers.";
                }
                combinationService.generateAndSaveCombinations(numbers);
            }
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }

        return "Combinations generated and saved to the database.";
    }


    @PostMapping("/removeConsecutiveNumbers")
    public String removeRecordsWithConsecutiveNumbers() {
        try {
            return combinationService.removeRecordsWithConsecutiveNumbers();
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }
    }

    @PostMapping("/removeExtendedCombinationsFromCombinationFour")
    public String removeExtendedCombinationsFromCombinationFour() {
        try {
            return combinationService.removeExtendedCombinationsFromCombinationFour();
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }
    }
    @PostMapping("/removeRowsContainingSubset")
    public ResponseEntity<String> removeRowsContainingSubset(@RequestBody List<Integer> subset) {
        try {
            combinationService.removeRowsContainingSubset(subset);
            return ResponseEntity.ok("Rows containing the subset have been successfully removed.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
        }
    }
}
