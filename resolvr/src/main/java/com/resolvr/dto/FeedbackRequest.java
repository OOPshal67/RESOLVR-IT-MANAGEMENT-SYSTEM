package com.resolvr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class FeedbackRequest {

    @Min(1)
    @Max(5)
    private int rating = 5;

    @Size(max = 4000)
    private String comments;
    @Size(max = 4000)
    private String closureNotes;

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public String getClosureNotes() { return closureNotes; }
    public void setClosureNotes(String closureNotes) { this.closureNotes = closureNotes; }
}
