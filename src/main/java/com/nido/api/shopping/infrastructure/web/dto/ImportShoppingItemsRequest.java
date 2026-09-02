package com.nido.api.shopping.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImportShoppingItemsRequest(@NotEmpty @Size(max = 200) @Valid List<ImportShoppingItemLineRequest> lines) {}
