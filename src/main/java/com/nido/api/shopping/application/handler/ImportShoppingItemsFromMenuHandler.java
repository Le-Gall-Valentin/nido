package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.ImportShoppingItemsFromMenuUseCase;
import com.nido.api.shopping.domain.model.AddShoppingItemCommand;
import com.nido.api.shopping.domain.model.ImportShoppingItemsCommand;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingImportLine;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.ShoppingItemNameNormalizer;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
public class ImportShoppingItemsFromMenuHandler implements ImportShoppingItemsFromMenuUseCase {

    private final ShoppingItemRepository itemRepository;
    private final ShoppingCategoryRepository categoryRepository;

    public ImportShoppingItemsFromMenuHandler(ShoppingItemRepository itemRepository, ShoppingCategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public List<ShoppingItem> importItems(ImportShoppingItemsCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();

        Set<UUID> validCategoryIds = categoryRepository.findBySpaceId(command.spaceId()).stream()
            .map(ShoppingCategory::id).collect(Collectors.toSet());

        // Normalized once per pending item up front, then looked up by key per line —
        // avoids renormalizing every pending item's name for every import line.
        // putIfAbsent: a new line added earlier in this same batch must be matchable by
        // a later line with the same normalized name too, and on a name collision the
        // earliest-seen item keeps matching, same as the original ordered scan did.
        Map<String, ShoppingItem> pendingByNormalizedName = new HashMap<>();
        itemRepository.findBySpaceIdAndDoneFalse(command.spaceId())
            .forEach(item -> pendingByNormalizedName.putIfAbsent(ShoppingItemNameNormalizer.normalize(item.name()), item));

        List<ShoppingItem> result = new ArrayList<>();
        for (ShoppingImportLine line : command.lines()) {
            if (!validCategoryIds.contains(line.categoryId())) {
                throw new ShoppingException.CategoryNotFound();
            }
            String normalized = ShoppingItemNameNormalizer.normalize(line.name());
            ShoppingItem match = pendingByNormalizedName.get(normalized);
            if (match != null) {
                ShoppingItem updated = itemRepository.update(new UpdateShoppingItemCommand(
                    match.id(), command.spaceId(), line.categoryId(), match.name(), line.quantity(), line.unit()));
                result.add(updated);
            } else {
                ShoppingItem created = itemRepository.add(new AddShoppingItemCommand(
                    command.spaceId(), line.categoryId(), line.name(), line.quantity(), line.unit()));
                result.add(created);
                pendingByNormalizedName.put(normalized, created);
            }
        }
        return result;
    }
}
