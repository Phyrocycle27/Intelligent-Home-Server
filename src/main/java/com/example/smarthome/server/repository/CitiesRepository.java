package com.example.smarthome.server.repository;

import com.example.smarthome.server.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitiesRepository extends JpaRepository<City, Integer> {

}
