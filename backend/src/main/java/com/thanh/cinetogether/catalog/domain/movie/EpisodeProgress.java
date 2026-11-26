package com.thanh.cinetogether.catalog.domain.movie;

/**
 * How far a series has come: the latest episode, the planned number of episodes and whether it is finished.
 * Each number is null when the provider does not say. A single movie has no episodes: it is completed with no numbers.
 */
public record EpisodeProgress(Integer current, Integer total, boolean completed) {

    public static final EpisodeProgress NONE = new EpisodeProgress(null, null, false);

}
