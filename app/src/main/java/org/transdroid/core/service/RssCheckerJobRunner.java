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
package org.transdroid.core.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.evernote.android.job.Job;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.rss.RssFeedsActivity_;
import org.transdroid.core.rssparser.Item;
import org.transdroid.core.rssparser.RssParser;
import org.transdroid.daemon.util.Collections2;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@EBean
public class RssCheckerJobRunner {

    @RootContext
    protected Context context;
    @Bean
    protected Log log;
    @Bean
    protected ConnectivityHelper connectivityHelper;
    @Bean
    protected NotificationSettings notificationSettings;
    @Bean
    protected ApplicationSettings applicationSettings;
    @SystemService
    protected NotificationManager notificationManager;

    Job.Result run() {

        if (!connectivityHelper.shouldPerformBackgroundActions() || !notificationSettings.isEnabledForRss()) {
            log.d(this,
                    "Skip the RSS checker service, as background data is disabled, the service is disabled or we are not connected.");
            return Job.Result.RESCHEDULE;
        }

        // Check every RSS feed for new items
        int unread = 0;
        Set<String> hasUnread = new LinkedHashSet<>();
        for (RssfeedSetting feed : applicationSettings.getRssfeedSettings()) {
            try {

                if (!feed.shouldAlarmOnNewItems()) {
                    log.d(this, "Skip checker for " + feed.getName() + " as alarms are disabled");
                    continue;
                }

                log.d(this, "Try to parse " + feed.getName() + " (" + feed.getUrl() + ")");
                RssParser parser = new RssParser(feed.getUrl(), feed.getExcludeFilter(), feed.getIncludeFilter());
                parser.parse();
                if (parser.getChannel() == null) {
                    continue;
                }

                // Find the last item that is newer than the last viewed date
                boolean usePublishDate = false;
                if (parser.getChannel().getItems().size() > 0) {
                    Date pubDate = parser.getChannel().getItems().get(0).getPubdate();
                    usePublishDate = pubDate != null && pubDate.getTime() > 0;
                }
                for (Item item : parser.getChannel().getItems()) {
                    if (usePublishDate
                            && item.getPubdate() != null
                            && item.getPubdate().before(feed.getLastViewed())) {
                        break;
                    } else if (!usePublishDate
                            && item.getTheLink() != null
                            && feed.getLastViewedItemUrl() != null
                            && item.getTheLink().equals(feed.getLastViewedItemUrl())) {
                        break;
                    } else {
                        unread++;
                        hasUnread.add(feed.getName());
                    }
                }

                log.d(this,
                        feed.getName() + " has " + (hasUnread.contains(feed.getName()) ? "" : "no ") + "unread items");

            } catch (Exception e) {
                // Ignore RSS feeds that could not be retrieved or parsed
            }
        }

        if (unread == 0) {
            // No new items; just exit
            return Job.Result.SUCCESS;
        }

        // Provide a notification, since there are new RSS items
        PendingIntent pi = PendingIntent
                .getActivity(context, 80000, new Intent(context, RssFeedsActivity_.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = context.getResources().getQuantityString(R.plurals.rss_service_new, unread, Integer.toString(unread));
        String forString = Collections2.joinString(hasUnread, ", ");
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.CHANNEL_RSS_CHECKER)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.rss_service_newfor, forString))
                .setNumber(unread).setLights(notificationSettings.getDesiredLedColour(), 600, 1000)
                .setSound(notificationSettings.getSound())
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (notificationSettings.shouldVibrate()) {
            builder.setVibrate(notificationSettings.getDefaultVibratePattern());
        }
        notificationManager.notify(80001, builder.build());

        return Job.Result.SUCCESS;
    }

}
