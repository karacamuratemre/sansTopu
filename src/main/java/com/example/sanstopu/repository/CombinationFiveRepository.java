package com.example.sanstopu.repository;



import com.example.sanstopu.entity.CombinationFive;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CombinationFiveRepository extends MongoRepository<CombinationFive, String> {

    List<CombinationFive> findAllByNumbersIn(List<String> combinationJsons);
}
