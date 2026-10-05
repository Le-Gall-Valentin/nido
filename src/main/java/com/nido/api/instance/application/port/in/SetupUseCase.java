package com.nido.api.instance.application.port.in;

import com.nido.api.instance.domain.model.CompleteSetupCommand;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SetupEncryptionKey;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** The first-run setup. Every call asks for the code; once the setup is done, every call answers as if absent. */
public interface SetupUseCase {
    void verifyCode(String code);
    SetupEncryptionKey encryptionKey(String code);
    void sendTestMail(String code, Map<SettingKey, String> mail, String publicUrl, String recipient, Locale locale);
    /** All or nothing: the administrator, the address, the mail, the end of the setup. Returns the administrator's id. */
    UUID complete(CompleteSetupCommand command);
}
