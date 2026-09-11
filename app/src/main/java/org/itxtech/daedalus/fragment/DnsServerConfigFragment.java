package org.itxtech.daedalus.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import com.google.android.material.snackbar.Snackbar;
import org.itxtech.daedalus.Daedalus;
import org.itxtech.daedalus.R;
import org.itxtech.daedalus.activity.ConfigActivity;
import org.itxtech.daedalus.server.CustomDnsServer;
import org.itxtech.daedalus.server.DnsServer;
import org.itxtech.daedalus.util.Logger;
import org.itxtech.daedalus.util.TlsCertificates;

import java.security.cert.X509Certificate;
import java.text.DateFormat;

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
public class DnsServerConfigFragment extends ConfigFragment {
    private static final int IMPORT_CERTIFICATE_REQUEST_CODE = 1;

    private int index;
    // Certificate chosen for this server; applied to the server together with the other fields
    private String certificate = null;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.perf_server);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        EditTextPreference serverName = findPreference("serverName");
        serverName.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary((String) newValue);
            return true;
        });

        EditTextPreference serverAddress = findPreference("serverAddress");
        serverAddress.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(withHint((String) newValue, getString(R.string.settings_server_address_summary)));
            return true;
        });

        EditTextPreference serverPort = findPreference("serverPort");
        serverPort.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(withHint((String) newValue, getString(R.string.settings_server_port_summary)));
            return true;
        });

        SwitchPreference serverProxied = findPreference("serverProxied");

        findPreference("serverCertificate").setOnPreferenceClickListener(preference -> {
            onCertificateClicked();
            return true;
        });

        index = intent.getIntExtra(ConfigActivity.LAUNCH_ACTION_ID, ConfigActivity.ID_NONE);
        if (index != ConfigActivity.ID_NONE) {
            CustomDnsServer server = Daedalus.configurations.getCustomDNSServers().get(index);
            serverName.setText(server.getName());
            serverName.setSummary(server.getName());
            serverAddress.setText(server.getAddress());
            serverAddress.setSummary(withHint(server.getAddress(), getString(R.string.settings_server_address_summary)));
            serverPort.setText(String.valueOf(server.getPort()));
            serverPort.setSummary(withHint(String.valueOf(server.getPort()), getString(R.string.settings_server_port_summary)));
            serverProxied.setChecked(server.isProxied());
            certificate = server.getCertificate();
        } else {
            serverName.setText("");
            serverAddress.setText("");
            serverAddress.setSummary(getString(R.string.settings_server_address_summary));
            String port = String.valueOf(DnsServer.DNS_SERVER_DEFAULT_PORT);
            serverPort.setText(port);
            serverPort.setSummary(withHint(port, getString(R.string.settings_server_port_summary)));
            serverProxied.setChecked(false);
            certificate = null;
        }
        updateCertificateSummary();
        return view;
    }

    private static String withHint(String value, String hint) {
        return value == null || value.trim().isEmpty() ? hint : value + "\n" + hint;
    }

    private void onCertificateClicked() {
        if (certificate == null) {
            pickCertificate();
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setItems(new CharSequence[]{getString(R.string.cert_replace), getString(R.string.cert_remove)},
                        (dialog, which) -> {
                            if (which == 0) {
                                pickCertificate();
                            } else {
                                certificate = null;
                                updateCertificateSummary();
                            }
                        })
                .show();
    }

    private void pickCertificate() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, IMPORT_CERTIFICATE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_CERTIFICATE_REQUEST_CODE || resultCode != Activity.RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        try {
            certificate = TlsCertificates.importFrom(getActivity().getContentResolver(), data.getData());
            updateCertificateSummary();
        } catch (Exception e) {
            Logger.logException(e);
            Snackbar.make(getView(), R.string.cert_import_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    private void updateCertificateSummary() {
        Preference preference = findPreference("serverCertificate");
        if (certificate == null) {
            preference.setSummary(R.string.cert_none);
            return;
        }
        try {
            X509Certificate first = TlsCertificates.load(certificate).get(0);
            preference.setSummary(TlsCertificates.getCommonName(first.getSubjectX500Principal()) + "\n"
                    + getString(R.string.cert_details,
                    TlsCertificates.getCommonName(first.getIssuerX500Principal()),
                    DateFormat.getDateInstance().format(first.getNotAfter())));
        } catch (Exception e) {
            Logger.logException(e);
            preference.setSummary(certificate);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_apply:
                String serverName = ((EditTextPreference) findPreference("serverName")).getText();
                String serverAddress = ((EditTextPreference) findPreference("serverAddress")).getText();
                String serverPort = ((EditTextPreference) findPreference("serverPort")).getText();
                boolean proxied = ((SwitchPreference) findPreference("serverProxied")).isChecked();

                if (serverName.equals("") | serverAddress.equals("") | serverPort.equals("")) {
                    Snackbar.make(getView(), R.string.notice_fill_in_all, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                }

                String previousCertificate = null;
                if (index == ConfigActivity.ID_NONE) {
                    CustomDnsServer server = new CustomDnsServer(serverName, serverAddress, Integer.parseInt(serverPort));
                    server.setProxied(proxied);
                    server.setCertificate(certificate);
                    Daedalus.configurations.getCustomDNSServers().add(server);
                } else {
                    CustomDnsServer server = Daedalus.configurations.getCustomDNSServers().get(index);
                    previousCertificate = server.getCertificate();
                    server.setName(serverName);
                    server.setAddress(serverAddress);
                    server.setPort(Integer.parseInt(serverPort));
                    server.setProxied(proxied);
                    server.setCertificate(certificate);
                }
                // Save right away rather than only when the activity is destroyed
                Daedalus.configurations.save();
                if (previousCertificate != null && !previousCertificate.equals(certificate)) {
                    TlsCertificates.deleteIfUnused(previousCertificate);
                }
                Daedalus.setRulesChanged();
                getActivity().finish();
                break;
            case R.id.action_delete:
                if (index != ConfigActivity.ID_NONE) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.notice_delete_confirm_prompt)
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                CustomDnsServer removed = Daedalus.configurations.getCustomDNSServers().remove(index);
                                Daedalus.configurations.save();
                                TlsCertificates.deleteIfUnused(removed.getCertificate());
                                getActivity().finish();
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .create()
                            .show();
                } else {
                    Daedalus.setRulesChanged();
                    getActivity().finish();
                }
                break;
        }

        return true;
    }
}
