package com.example.advertisement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Click implements Serializable {
    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("impression_id")
    private String impressionId;

    @JsonProperty("revenue")
    private Double revenue;
}
