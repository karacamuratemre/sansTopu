package com.example.sanstopu.service;

import com.example.sanstopu.entity.*;
import com.example.sanstopu.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class SansTopuCombinationService {


    @Autowired
    private CombinationOneRepository combinationOneRepository;
    @Autowired
    private CombinationTwoRepository combinationTwoRepository;
    @Autowired
    private CombinationThreeRepository combinationThreeRepository;
    @Autowired
    private CombinationFourRepository combinationFourRepository;
    @Autowired
    private CombinationFiveRepository combinationFiveRepository;
    @Autowired
    private CombinationSuperAllRepository combinationSuperAllRepository;

    private ObjectMapper objectMapper = new ObjectMapper();
    private static final int BATCH_SIZE = 10000; // Batch size for batch operations


    @Transactional
    public void generateAndSaveAllCombinations() {
        List<List<Integer>> combinations = generateFiveNumberCombinations(34, 5);

        List<CombinationSuperAll> batchToSave = new ArrayList<>();

        for (List<Integer> combination : combinations) {
            String combinationJson = convertToJson(combination);
            CombinationSuperAll newCombination = new CombinationSuperAll(combinationJson, 1);
            batchToSave.add(newCombination);

            if (batchToSave.size() >= BATCH_SIZE) {
                combinationSuperAllRepository.saveAll(batchToSave); // Toplu olarak kaydet
                batchToSave.clear();
            }
        }

        if (!batchToSave.isEmpty()) {
            combinationSuperAllRepository.saveAll(batchToSave); // Kalanları kaydet
        }
    }

    private List<List<Integer>> generateFiveNumberCombinations(int n, int k) {
        List<List<Integer>> combinations = new ArrayList<>();
        combineSuper(combinations, new ArrayList<>(), n, k, 1);
        return combinations;
    }

    private void combineSuper(List<List<Integer>> combinations, List<Integer> current, int n, int k, int start) {
        if (current.size() == k) {
            combinations.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i <= n; i++) {
            current.add(i);  // Kombinasyona sayıyı ekle
            combineSuper(combinations, current, n, k, i + 1);  // Sonraki kombinasyonlar için rekürsif çağrı
            current.remove(current.size() - 1);  // Son eklenen sayıyı çıkar ve diğer olasılıkları dene
        }
    }

    private String convertToJson(List<Integer> combination) {
        try {
            return objectMapper.writeValueAsString(combination);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    public long getCombinationAllCount() {
        return combinationSuperAllRepository.count();
    }


    @Transactional
    public String removeCombinations(List<List<Integer>> inputSets) {
        List<String> combinationsToDelete = inputSets.stream()
                .map(this::convertToJson)
                .collect(Collectors.toList());

        combinationSuperAllRepository.deleteByNumbersIn(combinationsToDelete);

        return "Specified combinations have been removed from the database.";
    }


    public void generateAndSaveCombinations(List<Integer> drawnNumbers) throws ExecutionException, InterruptedException {

        List<Callable<Void>> tasks = new ArrayList<>();

        // Create tasks for parallel processing
        tasks.add(() -> {
            processCombinations(drawnNumbers, 1, combinationOneRepository);
            return null;
        });
        tasks.add(() -> {
            processCombinations(drawnNumbers, 2, combinationTwoRepository);
            return null;
        });
        tasks.add(() -> {
            processCombinations(drawnNumbers, 3, combinationThreeRepository);
            return null;
        });
        tasks.add(() -> {
            processCombinations(drawnNumbers, 4, combinationFourRepository);
            return null;
        });
        tasks.add(() -> {
            processCombinations(drawnNumbers, 5, combinationFiveRepository);
            return null;
        });


        // Use ExecutorService for parallel processing
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> futures = executorService.invokeAll(tasks);

        for (Future<Void> future : futures) {
            future.get(); // Wait for all tasks to complete
        }

        executorService.shutdown();
    }


    private <T> void processCombinations(List<Integer> drawnNumbers, int setSize, MongoRepository<T, String> repository) throws JsonProcessingException {
        List<List<Integer>> combinations = generateCombinations(drawnNumbers, setSize);
        Map<String, Long> combinationCountMap = combinations.stream()
                .collect(Collectors.groupingBy(
                        this::convertToJson,
                        Collectors.counting()
                ));

        // Retrieve existing combinations in one query
        List<String> jsonCombinations = new ArrayList<>(combinationCountMap.keySet());
        Map<String, T> existingCombinationsMap = findExistingCombinations(repository, jsonCombinations);

        List<T> batchToSave = new ArrayList<>();

        for (Map.Entry<String, Long> entry : combinationCountMap.entrySet()) {
            String combinationJson = entry.getKey();
            int count = entry.getValue().intValue();

            T existingCombination = existingCombinationsMap.get(combinationJson);
            if (existingCombination != null) {
                updateCombinationCount(existingCombination, count);
                batchToSave.add(existingCombination);
            } else {
                T newCombination = createNewCombination(repository, combinationJson, count);
                batchToSave.add(newCombination);
            }

            if (batchToSave.size() >= BATCH_SIZE) {
                repository.saveAll(batchToSave); // Save in batch
                batchToSave.clear();
            }
        }

        if (!batchToSave.isEmpty()) {
            repository.saveAll(batchToSave); // Save remaining
        }
    }

    private List<List<Integer>> generateCombinations(List<Integer> numbers, int k) {
        List<List<Integer>> combinations = new ArrayList<>();
        combine(combinations, new ArrayList<>(), numbers, k, 0);
        return combinations;
    }
    private <T> T createNewCombination(MongoRepository<T, String> repository, String combinationJson, int count) {
        if (repository instanceof CombinationOneRepository) {
            return (T) new CombinationOne(combinationJson, count);
        } else if (repository instanceof CombinationTwoRepository) {
            return (T) new CombinationTwo(combinationJson, count);
        } else if (repository instanceof CombinationThreeRepository) {
            return (T) new CombinationThree(combinationJson, count);
        } else if (repository instanceof CombinationFourRepository) {
            return (T) new CombinationFour(combinationJson, count);
        }  else {
            return (T) new CombinationFive(combinationJson, count);
        }
    }
    private <T> void updateCombinationCount(T combination, int additionalCount) {
        if (combination instanceof CombinationOne) {
            CombinationOne combinationOne = (CombinationOne) combination;
            combinationOne.setCount(combinationOne.getCount() + additionalCount);
        } else if (combination instanceof CombinationTwo) {
            CombinationTwo combinationTwo = (CombinationTwo) combination;
            combinationTwo.setCount(combinationTwo.getCount() + additionalCount);
        } else if (combination instanceof CombinationThree) {
            CombinationThree combinationThree = (CombinationThree) combination;
            combinationThree.setCount(combinationThree.getCount() + additionalCount);
        } else if (combination instanceof CombinationFour) {
            CombinationFour combinationFour = (CombinationFour) combination;
            combinationFour.setCount(combinationFour.getCount() + additionalCount);
        } else if (combination instanceof CombinationFive) {
            CombinationFive combinationFive = (CombinationFive) combination;
            combinationFive.setCount(combinationFive.getCount() + additionalCount);
        }
    }
    private void combine(List<List<Integer>> combinations, List<Integer> current, List<Integer> numbers, int k, int start) {
        if (current.size() == k) {
            combinations.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < numbers.size(); i++) {
            current.add(numbers.get(i));  // Kombinasyona sayıyı ekle
            combine(combinations, current, numbers, k, i + 1);  // Sonraki kombinasyonlar için recursive çağrı
            current.remove(current.size() - 1);  // Son eklenen sayıyı çıkar ve diğer olasılıkları dene
        }
    }


    private <T> Map<String, T> findExistingCombinations(MongoRepository<T, String> repository, List<String> combinationJsons) {
        if (repository instanceof CombinationOneRepository) {
            List<CombinationOne> existingCombinations = ((CombinationOneRepository) repository).findAllByNumbersIn(combinationJsons);
            return existingCombinations.stream()
                    .collect(Collectors.toMap(CombinationOne::getNumbers, combination -> (T) combination));
        } else if (repository instanceof CombinationTwoRepository) {
            List<CombinationTwo> existingCombinations = ((CombinationTwoRepository) repository).findAllByNumbersIn(combinationJsons);
            return existingCombinations.stream()
                    .collect(Collectors.toMap(CombinationTwo::getNumbers, combination -> (T) combination));
        } else if (repository instanceof CombinationThreeRepository) {
            List<CombinationThree> existingCombinations = ((CombinationThreeRepository) repository).findAllByNumbersIn(combinationJsons);
            return existingCombinations.stream()
                    .collect(Collectors.toMap(CombinationThree::getNumbers, combination -> (T) combination));
        } else if (repository instanceof CombinationFourRepository) {
            List<CombinationFour> existingCombinations = ((CombinationFourRepository) repository).findAllByNumbersIn(combinationJsons);
            return existingCombinations.stream()
                    .collect(Collectors.toMap(CombinationFour::getNumbers, combination -> (T) combination));
        } else if (repository instanceof CombinationFiveRepository) {
            List<CombinationFive> existingCombinations = ((CombinationFiveRepository) repository).findAllByNumbersIn(combinationJsons);
            return existingCombinations.stream()
                    .collect(Collectors.toMap(CombinationFive::getNumbers, combination -> (T) combination));
        }
        return new HashMap<>();
    }

    @Transactional
    public String removeRecordsWithConsecutiveNumbers() {
        // Fetch all records from combinationSuperAll
        List<CombinationSuperAll> allCombinations = combinationSuperAllRepository.findAll();
        List<String> recordsToDelete = new ArrayList<>();

        // Iterate over each combination
        for (CombinationSuperAll combination : allCombinations) {
            String numbersJson = combination.getNumbers();
            List<Integer> numbersList = convertFromJson(numbersJson);

            if (containsConsecutiveFour(numbersList)) {
                recordsToDelete.add(numbersJson);
            }
            if(containsConsecutiveThree(numbersList)){
                recordsToDelete.add(numbersJson);
            }
        }

        // Delete all records containing consecutive numbers
        if (!recordsToDelete.isEmpty()) {
            combinationSuperAllRepository.deleteByNumbersIn(recordsToDelete);
        }

        return "Records containing sequences of four consecutive numbers have been removed.";
    }

    // Check if the list contains any sequence of four consecutive numbers
    private boolean containsConsecutiveFour(List<Integer> numbers) {
        Collections.sort(numbers);
        for (int i = 0; i <= numbers.size() - 4; i++) {
            if (numbers.get(i + 1) == numbers.get(i) + 1 &&
                    numbers.get(i + 2) == numbers.get(i) + 2 &&
                    numbers.get(i + 3) == numbers.get(i) + 3) {
                return true;
            }
        }
        return false;
    }
    private boolean containsConsecutiveThree(List<Integer> numbers) {
        Collections.sort(numbers);
        for (int i = 0; i <= numbers.size() - 3; i++) {
            if (numbers.get(i + 1) == numbers.get(i) + 1 &&
                    numbers.get(i + 2) == numbers.get(i) + 2) {
                return true;
            }
        }
        return false;
    }
    // Convert a JSON string to a list of integers
    private List<Integer> convertFromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting from JSON", e);
        }
    }

    @Transactional
    public String removeExtendedCombinationsFromCombinationFour() {
        // Fetch all combinations from combinationFour collection
        List<CombinationFour> combinationsFour = combinationFourRepository.findAll();

        // Iterate over each combination from combinationFour
        for (CombinationFour combinationFour : combinationsFour) {
            String numbersJson = combinationFour.getNumbers();
            List<Integer> numbersList = convertFromJson(numbersJson);

            // Generate all possible 5-number combinations by adding one new number (1-34)
            Set<String> extendedCombinationsToDelete = new HashSet<>();

            for (int i = 1; i <= 34; i++) {
                if (!numbersList.contains(i)) {
                    List<Integer> extendedCombination = new ArrayList<>(numbersList);
                    extendedCombination.add(i);
                    Collections.sort(extendedCombination);

                    String extendedCombinationJson = convertToJson(extendedCombination);
                    extendedCombinationsToDelete.add(extendedCombinationJson);
                }
            }

            // Delete all combinations from combinationSuperAll in one batch query
            if (!extendedCombinationsToDelete.isEmpty()) {
                combinationSuperAllRepository.deleteByNumbersIn(new ArrayList<>(extendedCombinationsToDelete));
            }
        }

        return "All possible extended combinations from CombinationFour have been processed and deleted if they existed.";
    }


    @Transactional
    public String removeRowsContainingSubset(List<Integer> subset) {
        // Alt kümeyi JSON formatına dönüştür
        String subsetJson = convertToJson(subset);

        // Tüm CombinationSuperAll kayıtlarını al
        List<CombinationSuperAll> allCombinations = combinationSuperAllRepository.findAll();

        // Silinecek kayıtların listesini oluştur
        List<String> recordsToDelete = new ArrayList<>();

        for (CombinationSuperAll combination : allCombinations) {
            String numbersJson = combination.getNumbers();
            List<Integer> numbersList = convertFromJson(numbersJson);

            // Eğer mevcut kombinasyon alt kümeyi içeriyorsa, silinecekler listesine ekle
            if (numbersList.containsAll(subset)) {
                recordsToDelete.add(numbersJson);
            }
        }

        // Alt kümeyi içeren tüm kayıtları sil
        if (!recordsToDelete.isEmpty()) {
            combinationSuperAllRepository.deleteByNumbersIn(recordsToDelete);
        }

        return "Specified subset-containing rows have been removed from the database.";
    }

}
