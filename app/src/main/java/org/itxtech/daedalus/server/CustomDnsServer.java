package org.itxtech.daedalus.server;

import org.itxtech.daedalus.Daedalus;

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
public class CustomDnsServer extends AbstractDnsServer {
    private String name;
    private String id;
    // Reached through the SOCKS5 proxy (Settings > SOCKS5 Proxy), e.g. a DNS server inside
    // an EasyTier subnet. Missing in configurations written by older versions, so false by default.
    private boolean proxied;
    // Certificate trusted for DoT connections to this server (self-signed or private CA), or null
    private String certificate;

    public CustomDnsServer(String name, String address, int port) {
        super(address, port);
        this.name = name;
        this.id = String.valueOf(Daedalus.configurations.getNextDnsId());
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isProxied() {
        return proxied;
    }

    public void setProxied(boolean proxied) {
        this.proxied = proxied;
    }

    @Override
    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}
