package com.example.advertisement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    @JsonProperty("app_id")
    private String appId;
    @JsonProperty("country_code")
    private String countryCode;
    @JsonProperty("recommended_advertiser_ids")
    private List<String> recommendedAdvertiserIds;
}
