package com.nido.api.instance.domain.model;

import java.util.Map;

/** Everything the setup screen collected, sent in one go. {@code mail} null: mail left for later. */
public record CompleteSetupCommand(String code, InitialAdmin admin, String publicUrl, Map<SettingKey, String> mail,
                                   boolean encryptionKeySaved) {
    @Override
    public String toString() {
        return "CompleteSetupCommand[code=***, admin=" + admin + ", publicUrl=" + publicUrl + ", mail=" + (mail == null ? "later" : "given")
            + ", encryptionKeySaved=" + encryptionKeySaved + "]";
    }
}
