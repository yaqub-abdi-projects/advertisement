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
public class Impression implements Serializable {
    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("app_id")
    private String appId;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("advertiser_id")
    private String advertiserId;
}
