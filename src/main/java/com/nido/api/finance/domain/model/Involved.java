package com.nido.api.finance.domain.model;

import com.nido.api.space.domain.model.Addressee;

/** A member told about a change to an operation, and where the change leaves them. */
public record Involved(Addressee reader, Involvement involvement) {
}
