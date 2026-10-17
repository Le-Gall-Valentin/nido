package com.nido.api.calendar.infrastructure.feed;

import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ExternalFeedHttpConfig {

    /** The local network is read from the settings at every download: opening it from the page needs no restart. */
    @Bean
    ExternalFeedDownloader externalFeedDownloader(GetEffectiveSettingsQuery settings) {
        return new HttpExternalFeedDownloaderAdapter(
            AddressPolicy.standard(() -> settings.current().flag(SettingKey.CALENDAR_ALLOW_PRIVATE_NETWORK)),
            SystemDefaultDnsResolver.INSTANCE, HttpExternalFeedDownloaderAdapter.Limits.standard());
    }
}
