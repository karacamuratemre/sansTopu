package com.example.sanstopu.repository;


import com.example.sanstopu.entity.CombinationOne;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CombinationOneRepository extends MongoRepository<CombinationOne, String> {

    List<CombinationOne> findAllByNumbersIn(List<String> combinationJsons);
}