package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.ExportCalendarUseCase;
import com.nido.api.calendar.application.service.CalendarFeedAssembler;
import com.nido.api.calendar.domain.model.CalendarFeed;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.IcalDocument;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.IcalWriter;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** A one-off copy of the calendar, as the caller reads it; a viewer may take one, having seen it all already. */
@ApplicationService
public class ExportCalendarHandler implements ExportCalendarUseCase {

    private final CalendarFeedAssembler assembler;
    private final IcalWriter writer;
    private final AccountLanguage language;
    private final GetSpaceTodayUseCase spaceToday;

    public ExportCalendarHandler(CalendarFeedAssembler assembler, IcalWriter writer, AccountLanguage language,
                                 GetSpaceTodayUseCase spaceToday) {
        this.assembler = assembler;
        this.writer = writer;
        this.language = language;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional(readOnly = true)
    public IcalDocument export(Set<CalendarSourceType> sources, boolean onlyMine, SpaceMembership caller) {
        CalendarFeed feed = assembler.assemble(caller, CalendarSourceType.checkExportable(sources), onlyMine);
        return new IcalDocument(feed.name(), spaceToday.today(caller.spaceId()),
            writer.write(feed, language.of(caller.userId())));
    }
}
