package com.artembredak.nextrade.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoinGeckoClient {

    @Value("${coingecko.api.base-url}")
    private String baseUrl;

    @Value("${coingecko.api.coins}")
    private String coinIds;

    private final RestTemplate restTemplate;

    /**
     * Fetches live market data for all configured coin IDs from CoinGecko.
     * Returns an empty list (never throws) so the scheduler remains resilient
     * to transient API failures, rate-limit responses, or network timeouts.
     */
    public List<CoinMarketData> fetchMarketData() {
        String url = baseUrl
                + "/coins/markets?vs_currency=usd&ids=" + coinIds
                + "&order=market_cap_desc&per_page=100&page=1";

        log.debug("Fetching CoinGecko market data: url={}", url);

        try {
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<CoinMarketData>>() {}
            );
            List<CoinMarketData> body = response.getBody();
            if (body == null) {
                log.warn("CoinGecko returned null body for market data request");
                return Collections.emptyList();
            }
            log.debug("CoinGecko returned {} coin(s)", body.size());
            return body;
        } catch (RestClientException ex) {
            log.warn("CoinGecko API call failed — skipping price update cycle: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoinMarketData(
            String id,
            String symbol,
            String name,
            @JsonProperty("image") String imageUrl,
            @JsonProperty("current_price") BigDecimal currentPrice,
            @JsonProperty("price_change_percentage_24h") BigDecimal priceChange24h,
            @JsonProperty("market_cap") BigDecimal marketCap,
            @JsonProperty("total_volume") BigDecimal volume24h,
            @JsonProperty("last_updated") OffsetDateTime lastUpdated
    ) {}
}