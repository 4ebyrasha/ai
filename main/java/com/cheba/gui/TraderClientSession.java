package com.cheba.gui;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

/**
 * Клиентское представление серверной торговой сессии.
 * <p>
 * Один экземпляр передаётся между всеми экранами торговли.
 * Закрытие сессии отправляется серверу только один раз.
 */
public final class TraderClientSession {

    private final String token;

    private boolean transitioning;
    private boolean closed;

    public TraderClientSession(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Session token cannot be empty");
        }

        this.token = token;
    }

    public String getToken() {
        return token;
    }

    /**
     * Помечает закрытие текущего экрана как переход
     * на другой экран той же торговой сессии.
     */
    public void beginTransition() {
        transitioning = true;
    }

    /**
     * Вызывается новым экраном после перехода.
     */
    public void completeTransition() {
        transitioning = false;
    }

    /**
     * Закрывает серверную сессию, если экран действительно закрывается.
     */
    public void onScreenClosed() {
        if (transitioning || closed) {
            return;
        }

        closed = true;

        Client.sendData(
                EnumPacketServer.TraderSessionClose,
                token
        );
    }
}
