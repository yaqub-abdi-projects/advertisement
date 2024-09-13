package com.example.advertisement.shell;

import com.example.advertisement.click.ClickService;
import com.example.advertisement.impression.ImpressionService;
import com.example.advertisement.metric.MetricsCalculatorService;
import com.example.advertisement.model.Metrics;
import com.example.advertisement.model.Recommendation;
import com.example.advertisement.recommendation.RecommendationService;
import com.example.advertisement.utils.ReactiveJsonWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import reactor.core.publisher.Flux;

@ShellComponent
@RequiredArgsConstructor
public class ShellService {

    private final ImpressionService impressionService;
    private final ClickService clickService;
    private final MetricsCalculatorService metricsCalculatorService;
    private final RecommendationService recommendationService;
    private final ReactiveJsonWriter jsonWriter;

    @ShellMethod(value = "Load impression and click data from file paths.", key = "load")
    public void loadData(String impressionFilePath, String clickFilePath) {
        impressionService.load(impressionFilePath).subscribe();
        clickService.load(clickFilePath).subscribe();
    }

    @ShellMethod(value = "Calculate and aggregate advertisement metrics.", key = "metrics")
    public void calculateMetrics(String path) {
        Flux<Metrics> metrics = metricsCalculatorService.metrics();
        jsonWriter.writeJsonFile(metrics, path).subscribe();
    }

    @ShellMethod(value = "Generate recommendations based on data.", key = "recommendations")
    public void generateRecommendations(String path, int numberAdvertisers) {
        Flux<Recommendation> recommendations = recommendationService.recommendations(numberAdvertisers);
        jsonWriter.writeJsonFile(recommendations, path).subscribe();
    }
}
