package com.example.smarthome.server.repository;

import com.example.smarthome.server.entity.WeatherUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherUsersRepository extends JpaRepository<WeatherUser, Long> {

}
