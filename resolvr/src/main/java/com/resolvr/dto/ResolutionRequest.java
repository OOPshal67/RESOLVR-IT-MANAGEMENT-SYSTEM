package com.resolvr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResolutionRequest {

    @NotBlank(message = "Root cause analysis is required")
    private String rootCause;

    @NotBlank(message = "Resolution summary is required")
    private String resolutionSummary;

    @Size(max = 8000)
    private String workaroundApplied;

    @Min(value = 1, message = "Time spent must be at least 1 minute")
    private int timeSpentMinutes = 30;

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
    public String getWorkaroundApplied() { return workaroundApplied; }
    public void setWorkaroundApplied(String workaroundApplied) { this.workaroundApplied = workaroundApplied; }
    public int getTimeSpentMinutes() { return timeSpentMinutes; }
    public void setTimeSpentMinutes(int timeSpentMinutes) { this.timeSpentMinutes = timeSpentMinutes; }
}
