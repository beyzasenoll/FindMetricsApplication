package com.assia.metrics.domain.model;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "metric",
        "values"
})
public class Result {

    @JsonProperty("values")
    private List<List<Double>> values;

    @JsonProperty("values")
    public List<List<Double>> getValues() {
        return values;
    }
    @JsonProperty("values")
    public void setValues(List<List<Double>> values) {
        this.values = values;
    }
}