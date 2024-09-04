package com.example.sanstopu.repository;



import com.example.sanstopu.entity.CombinationThree;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CombinationThreeRepository extends MongoRepository<CombinationThree, String> {

    List<CombinationThree> findAllByNumbersIn(List<String> combinationJsons);
}
