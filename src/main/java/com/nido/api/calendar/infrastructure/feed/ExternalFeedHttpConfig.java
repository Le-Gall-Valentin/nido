package com.nido.api.calendar.infrastructure.feed;

import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.SettingKey;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class ExternalFeedHttpConfig {

    /**
     * The local network is read from the settings at every download: opening it from the page needs no restart. nido's
     * own addresses, and those of the database and Redis it is configured with, stay closed even then.
     */
    @Bean
    ExternalFeedDownloader externalFeedDownloader(GetEffectiveSettingsQuery settings, Environment environment) {
        return new HttpExternalFeedDownloaderAdapter(policy(settings, environment), SystemDefaultDnsResolver.INSTANCE,
            HttpExternalFeedDownloaderAdapter.Limits.standard());
    }

    static AddressPolicy policy(GetEffectiveSettingsQuery settings, Environment environment) {
        List<String> services = new ArrayList<>(OwnAddresses.hostsOf(setting(environment, "spring.datasource.url")));
        services.addAll(OwnAddresses.hostsOf(setting(environment, "spring.data.redis.url")));
        return AddressPolicy.standard(() -> settings.current().flag(SettingKey.CALENDAR_ALLOW_PRIVATE_NETWORK),
            new OwnAddresses(services));
    }

    /** A setting given another way — a connection made by the tests' containers — names no service here. */
    private static String setting(Environment environment, String key) {
        try {
            return environment.getProperty(key);
        } catch (IllegalArgumentException unresolved) {
            return null;
        }
    }
}
