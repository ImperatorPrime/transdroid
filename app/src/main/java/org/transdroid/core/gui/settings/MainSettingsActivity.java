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
package org.transdroid.core.gui.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.transdroid.R;
import org.transdroid.core.app.search.SearchHelper;
import org.transdroid.core.app.search.SearchSite;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.app.settings.WebsearchSetting;
import org.transdroid.core.gui.TorrentsActivity_;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.search.BarcodeHelper;
import org.transdroid.core.gui.settings.RssfeedPreference.OnRssfeedClickedListener;
import org.transdroid.core.gui.settings.ServerPreference.OnServerClickedListener;
import org.transdroid.core.gui.settings.WebsearchPreference.OnWebsearchClickedListener;
import org.transdroid.core.seedbox.SeedboxPreference;
import org.transdroid.core.seedbox.SeedboxPreference.OnSeedboxClickedListener;
import org.transdroid.core.seedbox.SeedboxProvider;
import org.transdroid.core.seedbox.XirvikDediSettings;
import org.transdroid.core.seedbox.XirvikSemiSettings;
import org.transdroid.core.seedbox.XirvikSharedSettings;
import org.transdroid.core.seedbox.XirvikSharedSettingsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * The main activity that provides access to all application settings. It shows the configured serves, web search sites and RSS feeds along with other
 * general settings.
 *
 * @author Eric Kok
 */
@EActivity
public class MainSettingsActivity extends PreferenceCompatActivity {

    protected static final int DIALOG_ADDSEEDBOX = 0;
    @Bean
    protected NavigationHelper navigationHelper;
    @Bean
    protected ApplicationSettings applicationSettings;
    @Bean
    protected SearchHelper searchHelper;
    @Bean
    protected Log log;
    protected SharedPreferences prefs;
    private OnPreferenceClickListener onAddServer = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (navigationHelper.enableSeedboxes())
                showDialog(DIALOG_ADDSEEDBOX);
            else
                ServerSettingsActivity_.intent(MainSettingsActivity.this).start();
            return true;
        }
    };
    private OnPreferenceClickListener onAddWebsearch = preference -> {
        WebsearchSettingsActivity_.intent(MainSettingsActivity.this).start();
        return true;
    };
    private OnPreferenceClickListener onAddRssfeed = preference -> {
        RssfeedSettingsActivity_.intent(MainSettingsActivity.this).start();
        return true;
    };
    private OnPreferenceClickListener onBackgroundSettings = preference -> {
        NotificationSettingsActivity_.intent(MainSettingsActivity.this).start();
        return true;
    };
    private OnPreferenceClickListener onSystemSettings = preference -> {
        SystemSettingsActivity_.intent(MainSettingsActivity.this).start();
        return true;
    };
    private OnPreferenceClickListener onHelpSettings = preference -> {
        HelpSettingsActivity_.intent(MainSettingsActivity.this).start();
        return true;
    };
    private OnPreferenceClickListener onDonate = preference -> {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.donate_url))));
        return true;
    };
    private OnServerClickedListener onServerClicked = serverSetting -> ServerSettingsActivity_.intent(MainSettingsActivity.this).key(serverSetting.getOrder()).start();
    private OnSeedboxClickedListener onSeedboxClicked = (serverSetting, provider, seedboxOffset) -> {
        // NOTE: The seedboxOffset is the seedbox type-unique order that we need to supply uin the Extras bundle to
        // edit this specific seedbox
        startActivity(provider.getSettings().getSettingsActivityIntent(MainSettingsActivity.this).putExtra("key", seedboxOffset));
    };
    private OnWebsearchClickedListener onWebsearchClicked = websearchSetting -> WebsearchSettingsActivity_.intent(MainSettingsActivity.this).key(websearchSetting.getOrder()).start();
    private OnRssfeedClickedListener onRssfeedClicked = rssfeedSetting -> RssfeedSettingsActivity_.intent(MainSettingsActivity.this).key(rssfeedSetting.getOrder()).start();
    private OnClickListener onAddSeedbox = (dialog, which) -> {
        // Start the configuration activity for this specific chosen seedbox
        if (which == 0)
            ServerSettingsActivity_.intent(MainSettingsActivity.this).start();
        else if (which == SeedboxProvider.values().length + 1)
            BarcodeHelper.startBarcodeScanner(this, BarcodeHelper.ACTIVITY_BARCODE_ADDSERVER);
        else
            startActivity(SeedboxProvider.values()[which - 1].getSettings().getSettingsActivityIntent(MainSettingsActivity.this));
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: Settings are loaded in onResume()
    }

    @Override
    protected void onResume() {
        super.onResume();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        boolean enableSearchUi = navigationHelper.enableSearchUi();
        boolean enableRssUi = navigationHelper.enableRssUi();
        boolean enableDonateLink = !getString(R.string.donate_url).isEmpty();

        // Load the preference menu and attach actions
        addPreferencesFromResource(R.xml.pref_main);
        prefs = getPreferenceManager().getSharedPreferences();
        findPreference("header_addserver").setOnPreferenceClickListener(onAddServer);
        if (enableSearchUi) {
            findPreference("header_addwebsearch").setOnPreferenceClickListener(onAddWebsearch);
        }
        if (enableRssUi) {
            findPreference("header_addrssfeed").setOnPreferenceClickListener(onAddRssfeed);
        }
        findPreference("header_background").setOnPreferenceClickListener(onBackgroundSettings);
        findPreference("header_system").setOnPreferenceClickListener(onSystemSettings);
        findPreference("header_help").setOnPreferenceClickListener(onHelpSettings);
        if (enableDonateLink) {
            findPreference("header_donate").setOnPreferenceClickListener(onDonate);
        } else {
            getPreferenceScreen().removePreference(findPreference("header_donate"));
        }

        // Keep a list of the server codes and names (for default server selection)
        List<String> serverCodes = new ArrayList<>();
        List<String> serverNames = new ArrayList<>();
        serverCodes.add(Integer.toString(ApplicationSettings.DEFAULTSERVER_LASTUSED));
        serverCodes.add(Integer.toString(ApplicationSettings.DEFAULTSERVER_ASKONADD));
        serverNames.add(getString(R.string.pref_defaultserver_lastused));
        serverNames.add(getString(R.string.pref_defaultserver_askonadd));

        // Add existing servers
        List<ServerSetting> servers = applicationSettings.getNormalServerSettings();
        for (ServerSetting serverSetting : servers) {
            getPreferenceScreen()
                    .addPreference(new ServerPreference(this).setServerSetting(serverSetting).setOnServerClickedListener(onServerClicked));
            if (serverSetting.getUniqueIdentifier() != null) {
                serverCodes.add(Integer.toString(serverSetting.getOrder()));
                serverNames.add(serverSetting.getName());
            }
        }
        // Add seedboxes; serversOffset keeps an int to have all ServerSettings with unique ids, seedboxOffset is unique
        // only per seedbox type
        int orderOffset = servers.size();
        for (SeedboxProvider provider : SeedboxProvider.values()) {
            int seedboxOffset = 0;
            for (ServerSetting seedbox : provider.getSettings().getAllServerSettings(prefs, orderOffset)) {
                getPreferenceScreen().addPreference(new SeedboxPreference(this).setProvider(provider).setServerSetting(seedbox)
                        .setOnSeedboxClickedListener(onSeedboxClicked, seedboxOffset));
                orderOffset++;
                seedboxOffset++;
                if (seedbox.getUniqueIdentifier() != null) {
                    serverCodes.add(Integer.toString(seedbox.getOrder()));
                    serverNames.add(seedbox.getName());
                }
            }
        }
        // Allow selection of the default server
        ListPreference defaultServerPreference = (ListPreference) findPreference("header_defaultserver");
        defaultServerPreference.setEntries(serverNames.toArray(new String[0]));
        defaultServerPreference.setEntryValues(serverCodes.toArray(new String[0]));

        // Add existing RSS feeds
        if (!enableRssUi) {
            // RSS should be disabled
            getPreferenceScreen().removePreference(findPreference("header_rssfeeds"));
        } else {
            List<RssfeedSetting> rssfeeds = applicationSettings.getRssfeedSettings();
            for (RssfeedSetting rssfeedSetting : rssfeeds) {
                getPreferenceScreen()
                        .addPreference(new RssfeedPreference(this).setRssfeedSetting(rssfeedSetting).setOnRssfeedClickedListener(onRssfeedClicked));
            }
        }

        if (!enableSearchUi) {
            // Search should be disabled
            getPreferenceScreen().removePreference(findPreference("header_searchsites"));
            return;
        }

        // Add existing websearch sites
        List<WebsearchSetting> websearches = applicationSettings.getWebsearchSettings();
        for (WebsearchSetting websearchSetting : websearches) {
            getPreferenceScreen().addPreference(
                    new WebsearchPreference(this).setWebsearchSetting(websearchSetting).setOnWebsearchClickedListener(onWebsearchClicked));
        }

        // Construct list of all available search sites, in-app and web
        ListPreference setSite = (ListPreference) findPreference("header_setsearchsite");
        // Retrieve the available in-app search sites (using the Torrent Search package)
        List<SearchSite> searchsites = searchHelper.getAvailableSites();
        if (searchsites == null) {
            searchsites = new ArrayList<>();
        }
        List<String> siteNames = new ArrayList<>(websearches.size() + searchsites.size());
        List<String> siteValues = new ArrayList<>(websearches.size() + searchsites.size());
        for (SearchSite searchSite : searchsites) {
            siteNames.add(searchSite.getName());
            siteValues.add(searchSite.getKey());
        }
        for (WebsearchSetting websearch : websearches) {
            siteNames.add(websearch.getName());
            siteValues.add(websearch.getKey());
        }
        // Supply the Preference list names and values
        setSite.setEntries(siteNames.toArray(new String[0]));
        setSite.setEntryValues(siteValues.toArray(new String[0]));

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @OptionsItem(android.R.id.home)
    protected void navigateUp() {
        TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_ADDSEEDBOX) {
            // Open dialog to pick one of the supported seedbox providers (or a normal server)
            String[] seedboxes = new String[SeedboxProvider.values().length + 2];
            seedboxes[0] = getString(R.string.pref_addserver_normal);
            for (int i = 0; i < seedboxes.length - 2; i++) {
                seedboxes[i + 1] = getString(R.string.pref_seedbox_addseedbox, SeedboxProvider.values()[i].getSettings().getName());
            }
            seedboxes[seedboxes.length - 1] = getString(R.string.pref_seedbox_xirvikviaqr);
            return new AlertDialog.Builder(this).setItems(seedboxes, onAddSeedbox).create();
        }
        return null;
    }

    @OnActivityResult(BarcodeHelper.ACTIVITY_BARCODE_ADDSERVER)
    public void onServerBarcodeScanned(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            final String result = data.getStringExtra("SCAN_RESULT");
            final String format = data.getStringExtra("SCAN_RESULT_FORMAT");
            if (format == null || !format.equals("QR_CODE") || result == null || result.split("\n").length < 3) {
                SnackbarManager.show(Snackbar.with(this).text(R.string.pref_seedbox_xirvikscanerror).colorResource(R.color.red)
                        .type(SnackbarType.MULTI_LINE));
                return;
            }
            onServerBarcodeScanHandled(result.split("\n"));
        }
    }

    protected void onServerBarcodeScanHandled(String[] qrResult) {
        final String server = qrResult[0];
        final String token = qrResult[2];
        final String name = server.replace(".xirvik.com", "");

        new XirvikSharedSettingsActivity.RetrieveXirvikAutoConfTask(server, "", "", token) {
            @Override
            protected void onPostExecute(String result) {
                if (result == null) {
                    log.d(MainSettingsActivity.this, "Could not retrieve the Xirvik shared seedbox RPC mount point setting");
                }
                switch (qrResult[1]) {
                    case "P":
                        XirvikDediSettings xirvikDediSettings = new XirvikDediSettings();
                        xirvikDediSettings.saveServerSetting(getApplicationContext(), server, token, name);
                        onResume();
                        break;
                    case "N":
                        XirvikSemiSettings xirvikSemiSettings = new XirvikSemiSettings();
                        xirvikSemiSettings.saveServerSetting(getApplicationContext(), server, token, name);
                        onResume();
                        break;
                    case "RG":
                        XirvikSharedSettings xirvikSharedSettings = new XirvikSharedSettings();
                        xirvikSharedSettings.saveServerSetting(getApplicationContext(), server, token, result, name);
                        onResume();
                        break;
                    default:
                        SnackbarManager.show(Snackbar.with(MainSettingsActivity.this).text(R.string.pref_seedbox_xirvikscanerror).colorResource(R.color.red).type(SnackbarType.MULTI_LINE));
                        break;
                }
            }
        }.execute();
    }


}
