package com.example.advertisement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Metrics implements Serializable {
    @JsonProperty("app_id")
    private String appId;
    @JsonProperty("country_code")
    private String countryCode;
    private Long impressions;
    private Long clicks;
    private Double revenue;
}
