//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "my.mail.ru" }, urls = { "http://my\\.mail\\.ru/jdeatme\\d+|http://my\\.mail\\.ru/video/(top#video=/[a-z0-9\\-_]+/[a-z0-9\\-_]+/[a-z0-9\\-_]+/\\d+|[a-z0-9\\-_]+/[a-z0-9\\-_]+/[a-z0-9\\-_]+/\\d+\\.html)" }, flags = { 2 })
public class MyMailRu extends PluginForHost {

    public MyMailRu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://my.mail.ru/";
    }

    private String              DLLINK         = null;
    private static final String TYPE_VIDEO_ALL = "http://my\\.mail\\.ru/video/(top#video=/[a-z0-9\\-_]+/[a-z0-9\\-_]+/[a-z0-9\\-_]+/\\d+|[a-z0-9\\-_]+/[a-z0-9\\-_]+/[a-z0-9\\-_]+/\\d+\\.html)";
    private static final String TYPE_VIDEO_1   = "http://my\\.mail\\.ru/video/top#video=/[a-z0-9\\-_]+/[a-z0-9\\-_]+/[a-z0-9\\-_]+/\\d+";
    private static final String TYPE_VIDEO_2   = "http://my\\.mail\\.ru/video/[a-z0-9\\-_]+/[a-z0-9\\-_]+/[a-z0-9\\-_]+/\\d+\\.html";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (link.getDownloadURL().matches(TYPE_VIDEO_ALL)) {
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            final String urlpart = getUrlpart(link);
            br.getPage("http://my.mail.ru/video/" + urlpart + ".html?ajax=photoitem&ajax_call=1&func_name=&mna=&mnb=&encoding=windows-1251");
            if (br.containsHTML("b\\-video__layer\\-error")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            if (br.containsHTML("class=\"unauthorised\\-user")) {
                link.getLinkStatus().setStatusText("Private video");
                return AvailableStatus.TRUE;
            }
            final String signvideourl = getJson("signVideoUrl");
            final String filename = getJson("videoTitle");
            if (signvideourl == null || filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("http://videoapi.my.mail.ru" + signvideourl);
            getVideoURL();
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            final String originalLink = link.getStringProperty("mainlink", null);
            // final Regex linkInfo = new Regex(originalLink, "\\.mail\\.ru/([^<>\"/]*?)/([^<>\"/]*?)/([^<>\"/]*?)/(\\d+)\\.html");
            final String fid = new Regex(originalLink, "(\\d+)\\.html$").getMatch(0);
            br.getPage(originalLink);
            if (br.containsHTML(">Данная страница не найдена на нашем сервере")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            for (int i = 1; i <= 2; i++) {
                if (i == 1) {
                    DLLINK = br.getRegex("data\\-filedimageurl=\"(http://[^<>\"]+\\-" + fid + "[^<>\"]*?)\"").getMatch(0);
                }
                // if (DLLINK == null) DLLINK = "http://content.foto.mail.ru/" + linkInfo.getMatch(0) + "/" + linkInfo.getMatch(1) + "/" +
                // linkInfo.getMatch(2) + "/i-" + linkInfo.getMatch(3) + link.getStringProperty("ext", null);
                if (DLLINK == null) {
                    continue;
                }
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(DLLINK);
                    if (con.getResponseCode() == 500) {
                        logger.info("High quality link is invalid, using normal link...");
                        DLLINK = null;
                        continue;
                    }
                    if (!con.getContentType().contains("html")) {
                        link.setDownloadSize(con.getLongContentLength());
                        link.setFinalFileName(fid + link.getStringProperty("ext", null));
                        break;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        int maxChunks = 1;
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(TYPE_VIDEO_ALL)) {
            if (br.containsHTML("class=\"unauthorised\\-user")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Private video");
            }
            maxChunks = 0;
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean resume = true;
        if (downloadLink.getBooleanProperty("noresume", false)) {
            resume = false;
        }
        // More chunks possible but not needed because we're only downloading
        // pictures here
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 416) {
                if (downloadLink.getBooleanProperty("noresume", false)) {
                    downloadLink.setProperty("noresume", Boolean.valueOf(false));
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error");
                }
                logger.info("Resume impossible, disabling it for the next try");
                downloadLink.setChunksProgress(null);
                downloadLink.setProperty("noresume", Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY, "Resume failed");
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // fixFilename(downloadLink);
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://my.mail.ru";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                final String[] userSplit = account.getUser().split("@");
                if (userSplit.length != 2) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.postPage("https://auth.mail.ru/cgi-bin/auth", "level=1&page=http%3A%2F%2Fmy.mail.ru%2F&Login=" + Encoding.urlEncode(userSplit[0]) + "&Domain=" + Encoding.urlEncode(userSplit[1]) + "&Password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "Mpop") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(br, account, false);
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // More chunks possible but not needed because we're only downloading
        // pictures here
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fixFilename(link);
        dl.startDownload();
    }

    private void fixFilename(final DownloadLink downloadLink) {
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        final String newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
        if (newExtension != null && !downloadLink.getFinalFileName().endsWith(newExtension)) {
            final String oldExtension = downloadLink.getFinalFileName().substring(downloadLink.getFinalFileName().lastIndexOf("."));
            if (oldExtension != null) {
                downloadLink.setFinalFileName(downloadLink.getFinalFileName().replace(oldExtension, newExtension));
            } else {
                downloadLink.setFinalFileName(downloadLink.getFinalFileName() + newExtension);
            }
        }
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":([0-9\\.]+)").getMatch(0);
        if (result == null) {
            result = br.getRegex("\"" + parameter + "\"([\t\n\r ]+)?:([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(2);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private String generateVideoUrl_old(final DownloadLink dl) throws IOException {
        final Regex urlparts = new Regex(dl.getDownloadURL(), "video=/([^<>\"/]*?)/([^<>\"/]*?)/([^<>\"/]*?)/([^<>\"/]+)");
        br.getPage("http://video.mail.ru/" + urlparts.getMatch(0) + "/" + urlparts.getMatch(1) + "/" + urlparts.getMatch(2) + "/" + urlparts.getMatch(3) + ".lite");
        final String srv = grabVar("srv");

        final String vcontentHost = grabVar("vcontentHost");
        final String key = grabVar("key");
        final String rnd = "abcde";
        final String rk = rnd + key;
        final String tempHash = JDHash.getMD5(rk);
        final String pk = tempHash.substring(0, 9) + rnd;
        DLLINK = "http://" + vcontentHost + "/" + urlparts.getMatch(0) + "/" + urlparts.getMatch(1) + "/" + urlparts.getMatch(2) + "/" + urlparts.getMatch(3) + "flv?k=" + pk + "&" + srv;
        return DLLINK;
    }

    private String grabVar(final String var) {
        return br.getRegex("\\$" + var + "=([^<>\"]*?)(\r|\t|\n)").getMatch(0);
    }

    private String getVideoURL() {
        final String[] qualities = { "hd", "md", "sd" };
        for (final String quality : qualities) {
            DLLINK = br.getRegex("\"name\":\"" + quality + "\",\"url\":\"(http://[^<>\"]*?)\"").getMatch(0);
            if (DLLINK != null) {
                break;
            }
        }
        return DLLINK;
    }

    private String getUrlpart(final DownloadLink dl) {
        if (dl.getDownloadURL().matches(TYPE_VIDEO_1)) {
            return new Regex(dl.getDownloadURL(), "video=/(.+)").getMatch(0);
        } else {
            return new Regex(dl.getDownloadURL(), "video/(.+)\\.html").getMatch(0);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}