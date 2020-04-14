package com.example.smarthome.server.entity;

import lombok.*;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "cities")
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "name", "suburb", "state", "countryCode"})
@AllArgsConstructor
@NoArgsConstructor
@Proxy(lazy = false)
public class City {

    @Id
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String suburb;

    @Column(nullable = false, name = "county_code")
    private String countryCode;
}
