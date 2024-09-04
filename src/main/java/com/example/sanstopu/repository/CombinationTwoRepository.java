package com.example.sanstopu.repository;



import com.example.sanstopu.entity.CombinationTwo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CombinationTwoRepository  extends MongoRepository<CombinationTwo, String> {


    List<CombinationTwo> findAllByNumbersIn(List<String> combinationJsons);
}