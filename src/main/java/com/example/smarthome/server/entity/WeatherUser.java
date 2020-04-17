package com.example.smarthome.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "weather_service_user")
@EqualsAndHashCode(of = {"id"})
@Proxy(lazy = false)
@AllArgsConstructor
@NoArgsConstructor
public class WeatherUser {

    @Id
    private long id;

    @JoinTable(name = "user_cities",
            joinColumns = @JoinColumn(
                    name = "user_id",
                    referencedColumnName = "id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "city_id",
                    referencedColumnName = "id"
            ))
    @ManyToMany(fetch = FetchType.EAGER)
    private List<City> cities;
}
