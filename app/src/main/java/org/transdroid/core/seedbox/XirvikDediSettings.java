/*
 * Copyright 2010-2018 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.seedbox;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.OS;

/**
 * Implementation of {@link SeedboxSettings} for the Xirvik dedicated seedbox.
 *
 * @author Eric Kok
 */
public class XirvikDediSettings extends SeedboxSettingsImpl implements SeedboxSettings {

    @Override
    public String getName() {
        return "Xirvik dedicated";
    }

    @Override
    public ServerSetting getServerSetting(SharedPreferences prefs, int orderOffset, int order) {
        // @formatter:off
        String server = prefs.getString("seedbox_xirvikdedi_server_" + order, null);
        if (server == null) {
            return null;
        }
        Daemon type = Daemon.fromCode(prefs.getString("seedbox_xirvikdedi_client_" + order, Daemon.toCode(Daemon.rTorrent)));
        String user = prefs.getString("seedbox_xirvikdedi_user_" + order, null);
        String pass = prefs.getString("seedbox_xirvikdedi_pass_" + order, null);
        String authToken = prefs.getString("seedbox_xirvikdedi_token_" + order, null);
        return new ServerSetting(
                orderOffset + order,
                prefs.getString("seedbox_xirvikdedi_name_" + order, null),
                type,
                server,
                null,
                0,
                null,
                type == Daemon.uTorrent ? 5010 : 443,
                type != Daemon.uTorrent,
                type != Daemon.uTorrent,
                false,
                null,
                type == Daemon.Deluge ? "/deluge" : null,
                true,
                user,
                pass,
                type == Daemon.Deluge ? "deluge" : null,
                authToken,
                OS.Linux,
                type == Daemon.uTorrent ? "/downloads" : null,
                "ftp://" + user + "@" + server + "/",
                pass,
                6,
                prefs.getBoolean("seedbox_xirvikdedi_alarmfinished_" + order, true),
                prefs.getBoolean("seedbox_xirvikdedi_alarmnew_" + order, false),
                prefs.getString("seedbox_xirvikdedi_alarmexclude_" + order, null),
                prefs.getString("seedbox_xirvikdedi_alarminclude_" + order, null),
                true);
        // @formatter:on
    }

    @Override
    public Intent getSettingsActivityIntent(Context context) {
        return XirvikDediSettingsActivity_.intent(context).get();
    }

    @Override
    public int getMaxSeedboxOrder(SharedPreferences prefs) {
        return getMaxSeedboxOrder(prefs, "seedbox_xirvikdedi_server_");
    }

    @Override
    public void removeServerSetting(SharedPreferences prefs, int order) {
        removeServerSetting(prefs, "seedbox_xirvikdedi_server_", new String[]{"seedbox_xirvikdedi_name_",
                "seedbox_xirvikdedi_server_", "seedbox_xirvikdedi_client_", "seedbox_xirvikdedi_user_",
                "seedbox_xirvikdedi_pass_", "seedbox_xirvikdedi_token_"}, order);
    }

    public void saveServerSetting(Context context, String server, String token, String name) {
        // Get server order
        int key = SeedboxProvider.XirvikDedi.getSettings().getMaxSeedboxOrder(PreferenceManager.getDefaultSharedPreferences(context)) + 1;

        // Shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Check server already exists to replace token
        for (int i = 0; i <= SeedboxProvider.XirvikDedi.getSettings().getMaxSeedboxOrder(PreferenceManager.getDefaultSharedPreferences(context)); i++) {
            if (prefs.getString("seedbox_xirvikdedi_server_" + i, "").equals(server)) {
                key = i;
            }
        }

        // Store new seedbox pref
        prefs.edit()
                .putString("seedbox_xirvikdedi_client_" + key, Daemon.toCode(Daemon.rTorrent))
                .putString("seedbox_xirvikdedi_name_" + key, name)
                .putString("seedbox_xirvikdedi_server_" + key, server)
                .putString("seedbox_xirvikdedi_user_" + key, "")
                .putString("seedbox_xirvikdedi_pass_" + key, "")
                .putString("seedbox_xirvikdedi_token_" + key, token)
                .apply();

    }

}
