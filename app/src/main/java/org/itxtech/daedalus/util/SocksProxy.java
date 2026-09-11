package org.itxtech.daedalus.util;

import android.content.SharedPreferences;
import org.itxtech.daedalus.Daedalus;
import org.itxtech.daedalus.server.AbstractDnsServer;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;

/**
 * Daedalus Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

/**
 * The SOCKS5 proxy configured in Settings > SOCKS5 Proxy. It is used only for custom
 * DNS servers that have "Connect through SOCKS5 proxy" enabled, e.g. a DNS server inside
 * an EasyTier subnet exposed by EasyTier's SOCKS5 server in non-TUN mode. SOCKS5 CONNECT
 * only carries TCP, so plain UDP queries to such a server are sent as DNS over TCP.
 */
public class SocksProxy {
    public static final String PREF_HOST = "settings_socks5_host";
    public static final String PREF_PORT = "settings_socks5_port";
    public static final String PREF_USERNAME = "settings_socks5_username";
    public static final String PREF_PASSWORD = "settings_socks5_password";

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 1080;

    public static boolean isProxied(AbstractDnsServer server) {
        return server != null && server.isProxied();
    }

    /**
     * The configured proxy, regardless of which servers use it.
     */
    public static Proxy getProxy() {
        SharedPreferences prefs = Daedalus.getPrefs();
        String host = prefs.getString(PREF_HOST, DEFAULT_HOST);
        host = host == null ? "" : host.trim();
        if (host.isEmpty()) {
            host = DEFAULT_HOST;
        }
        int port = DEFAULT_PORT;
        try {
            port = Integer.parseInt(prefs.getString(PREF_PORT, String.valueOf(DEFAULT_PORT)).trim());
        } catch (Exception e) {
            Logger.warning("Invalid SOCKS5 proxy port, using " + DEFAULT_PORT);
        }
        return new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(host, port));
    }

    /**
     * The proxy to reach the given server through, or null for a direct connection.
     */
    public static Proxy getProxy(AbstractDnsServer server) {
        return isProxied(server) ? getProxy() : null;
    }

    /**
     * An unconnected TCP socket for the given server, going through the proxy when the
     * server is configured to use it.
     */
    public static Socket createSocket(AbstractDnsServer server) {
        Proxy proxy = getProxy(server);
        return proxy == null ? new Socket() : new Socket(proxy);
    }

    /**
     * Answers SOCKS5 username/password requests of java.net sockets (and therefore of
     * OkHttp) with the configured credentials. Call once at application start.
     */
    public static void installAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (!"SOCKS5".equalsIgnoreCase(getRequestingProtocol())) {
                    return null;
                }
                SharedPreferences prefs = Daedalus.getPrefs();
                String username = prefs.getString(PREF_USERNAME, "");
                if (username == null || username.isEmpty()) {
                    return null;
                }
                String password = prefs.getString(PREF_PASSWORD, "");
                return new PasswordAuthentication(username, (password == null ? "" : password).toCharArray());
            }
        });
    }
}
