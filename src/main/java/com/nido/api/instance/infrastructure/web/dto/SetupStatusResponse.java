package com.nido.api.instance.infrastructure.web.dto;

/** Whether the setup screen is due; once the installation is set up, nothing else. */
public record SetupStatusResponse(boolean required, String lockedPublicUrl, boolean mailLocked) {}
