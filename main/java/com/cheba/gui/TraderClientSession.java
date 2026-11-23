package com.cheba.gui;

import com.cheba.trader.TraderPreset;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

/**
 * Клиентское представление независимой торговой сессии.
 */
public final class TraderClientSession {

    private static TraderClientSession activeSession;

    private final String token;
    private final String profileName;
    private final TraderPreset preset;

    private boolean transitioning;
    private boolean closed;

    public TraderClientSession(
            String token,
            String profileName,
            TraderPreset preset
    ) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Session token cannot be empty");
        }

        this.token = token;
        this.profileName = profileName == null ? "" : profileName;
        this.preset = preset;

        activeSession = this;
    }

    public String getToken() {
        return token;
    }

    public String getProfileName() {
        return profileName;
    }

    public TraderPreset getPreset() {
        return preset;
    }

    public static TraderClientSession getActiveSession() {
        return activeSession;
    }

    public void beginTransition() {
        transitioning = true;
    }

    public void completeTransition() {
        transitioning = false;
    }

    public void onScreenClosed() {
        if (transitioning || closed) {
            return;
        }

        closed = true;

        if (activeSession == this) {
            activeSession = null;
        }

        Client.sendData(
                EnumPacketServer.TraderSessionClose,
                token
        );
    }
}
