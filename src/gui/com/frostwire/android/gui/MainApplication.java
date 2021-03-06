/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui;

import android.app.Application;
import android.content.Context;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ViewConfiguration;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.android.util.HttpResponseCache;
import com.frostwire.android.util.ImageLoader;
import com.frostwire.bittorrent.BTContext;
import com.frostwire.bittorrent.BTEngine;
import com.frostwire.logging.Logger;
import com.frostwire.search.CrawlPagedWebSearchPerformer;
import com.frostwire.util.DirectoryUtils;
import org.gudy.azureus2.core3.util.protocol.AzURLStreamHandlerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

/**
 * @author gubatron
 * @author aldenml
 */
public class MainApplication extends Application {

    private static final Logger LOG = Logger.getLogger(MainApplication.class);

    @Override
    public void onCreate() {
        super.onCreate();

        ignoreHardwareMenu();

        try {
            HttpResponseCache.install(this);
        } catch (IOException e) {
            LOG.error("Unable to install global http cache", e);
        }

        com.frostwire.android.util.ImageLoader.getInstance(this);
        CrawlPagedWebSearchPerformer.setCache(new DiskCrawlCache(this));
        CrawlPagedWebSearchPerformer.setMagnetDownloader(new LibTorrentMagnetDownloader());

        try {

            //            if (!Librarian.instance().isExternalStorageMounted() || instance != null) {
            //                return;
            //            }

            // important initial setup here
            ConfigurationManager.create(this);

            setupBTEngine();

            NetworkManager.create(this);
            Librarian.create(this);
            Engine.create(this);

            LocalSearchEngine.create(getDeviceId());//getAndroidId());

            // to alleviate a little if the external storage is not mounted
            if (com.frostwire.android.util.SystemUtils.isPrimaryExternalStorageMounted()) {
                DirectoryUtils.deleteFolderRecursively(SystemUtils.getTempDirectory());
            }

            Librarian.instance().syncMediaStore();
            Librarian.instance().syncApplicationsProvider();
        } catch (Throwable e) {
            String stacktrace = Log.getStackTraceString(e);
            throw new RuntimeException("MainApplication Create exception: " + stacktrace, e);
        }
    }

    private String getDeviceId() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = tm.getDeviceId();

        //probably it's a tablet... Sony's tablet returns null here.
        if (deviceId == null) {
            deviceId = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        }
        return deviceId;
    }

    @Override
    public void onLowMemory() {
        ImageLoader.getInstance(this).clear();
        super.onLowMemory();
    }

    private void ignoreHardwareMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field f = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (f != null) {
                f.setAccessible(true);
                f.setBoolean(config, false);
            }
        } catch (Throwable ex) {
            // Ignore
        }
    }

    private void setupBTEngine() {
        // this hack is only due to the remaining vuze TOTorrent code
        URL.setURLStreamHandlerFactory(new AzURLStreamHandlerFactory());

        BTContext ctx = new BTContext();
        ctx.homeDir = SystemUtils.getLibTorrentDirectory(this);
        ctx.torrentsDir = SystemUtils.getTorrentsDirectory();
        ctx.dataDir = SystemUtils.getTorrentDataDirectory();
        ctx.port0 = 0;
        ctx.port1 = 0;
        ctx.iface = "0.0.0.0";
        ctx.optimizeMemory = true;

        BTEngine.ctx = ctx;

        BTEngine.getInstance().start();
    }
}
