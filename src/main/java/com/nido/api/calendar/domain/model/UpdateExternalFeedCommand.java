package com.nido.api.calendar.domain.model;

/** What a member changes of a calendar followed: its name — none, or blank, keeps the one it has — and its colour. */
public record UpdateExternalFeedCommand(String name, String color) {}
