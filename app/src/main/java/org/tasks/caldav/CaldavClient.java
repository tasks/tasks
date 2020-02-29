package org.tasks.caldav;

import static android.text.TextUtils.isEmpty;
import static at.bitfire.dav4jvm.XmlUtils.NS_CALDAV;
import static at.bitfire.dav4jvm.XmlUtils.NS_CARDDAV;
import static at.bitfire.dav4jvm.XmlUtils.NS_WEBDAV;

import android.content.Context;
import at.bitfire.cert4android.CustomCertManager;
import at.bitfire.cert4android.CustomCertManager.CustomHostnameVerifier;
import at.bitfire.dav4jvm.BasicDigestAuthHandler;
import at.bitfire.dav4jvm.DavResource;
import at.bitfire.dav4jvm.Property.Name;
import at.bitfire.dav4jvm.Response;
import at.bitfire.dav4jvm.Response.HrefRelation;
import at.bitfire.dav4jvm.XmlUtils;
import at.bitfire.dav4jvm.exception.DavException;
import at.bitfire.dav4jvm.exception.HttpException;
import at.bitfire.dav4jvm.property.CalendarHomeSet;
import at.bitfire.dav4jvm.property.CurrentUserPrincipal;
import at.bitfire.dav4jvm.property.DisplayName;
import at.bitfire.dav4jvm.property.GetCTag;
import at.bitfire.dav4jvm.property.ResourceType;
import at.bitfire.dav4jvm.property.SupportedCalendarComponentSet;
import at.bitfire.dav4jvm.property.SyncToken;
import com.todoroo.astrid.helper.UUIDHelper;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.internal.tls.OkHostnameVerifier;
import org.tasks.DebugNetworkInterceptor;
import org.tasks.R;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.security.Encryption;
import org.tasks.ui.DisplayableException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import timber.log.Timber;

public class CaldavClient {

  private final Encryption encryption;
  private final Preferences preferences;
  private final DebugNetworkInterceptor interceptor;
  private final OkHttpClient httpClient;
  private final HttpUrl httpUrl;
  private final Context context;
  private final BasicDigestAuthHandler basicDigestAuthHandler;
  private boolean foreground;

  @Inject
  CaldavClient(
      @ForApplication Context context,
      Encryption encryption,
      Preferences preferences,
      DebugNetworkInterceptor interceptor) {
    this.context = context;
    this.encryption = encryption;
    this.preferences = preferences;
    this.interceptor = interceptor;
    httpClient = null;
    httpUrl = null;
    basicDigestAuthHandler = null;
  }

  private CaldavClient(
      Context context,
      Encryption encryption,
      Preferences preferences,
      DebugNetworkInterceptor interceptor,
      String url,
      String username,
      String password,
      boolean foreground)
      throws NoSuchAlgorithmException, KeyManagementException {
    this.context = context;
    this.encryption = encryption;
    this.preferences = preferences;
    this.interceptor = interceptor;

    CustomCertManager customCertManager = new CustomCertManager(context);
    customCertManager.setAppInForeground(foreground);
    CustomHostnameVerifier hostnameVerifier =
        customCertManager.hostnameVerifier(OkHostnameVerifier.INSTANCE);
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {customCertManager}, null);

    basicDigestAuthHandler = new BasicDigestAuthHandler(null, username, password);
    Builder builder =
        new OkHttpClient()
            .newBuilder()
            .addNetworkInterceptor(basicDigestAuthHandler)
            .authenticator(basicDigestAuthHandler)
            .cookieJar(new MemoryCookieStore())
            .followRedirects(false)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.getSocketFactory(), customCertManager)
            .hostnameVerifier(hostnameVerifier)
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS);
    if (preferences.isFlipperEnabled()) {
      interceptor.add(builder);
    }
    httpClient = builder.build();
    httpUrl = HttpUrl.parse(url);
  }

  public CaldavClient forAccount(CaldavAccount account)
      throws NoSuchAlgorithmException, KeyManagementException {
    return forUrl(account.getUrl(), account.getUsername(), account.getPassword(encryption));
  }

  CaldavClient forCalendar(CaldavAccount account, CaldavCalendar calendar)
      throws NoSuchAlgorithmException, KeyManagementException {
    return forUrl(calendar.getUrl(), account.getUsername(), account.getPassword(encryption));
  }

  CaldavClient forUrl(String url, String username, String password)
      throws KeyManagementException, NoSuchAlgorithmException {
    return new CaldavClient(
        context, encryption, preferences, interceptor, url, username, password, foreground);
  }

  private String tryFindPrincipal(String link) throws DavException, IOException {
    HttpUrl url = httpUrl.resolve(link);
    Timber.d("Checking for principal: %s", url);
    DavResource davResource = new DavResource(httpClient, url);
    ResponseList responses = new ResponseList();
    davResource.propfind(0, new Name[] {CurrentUserPrincipal.NAME}, responses);
    if (!responses.isEmpty()) {
      Response response = responses.get(0);
      CurrentUserPrincipal currentUserPrincipal = response.get(CurrentUserPrincipal.class);
      if (currentUserPrincipal != null) {
        String href = currentUserPrincipal.getHref();
        if (!isEmpty(href)) {
          return href;
        }
      }
    }
    return null;
  }

  private String findHomeset() throws DavException, IOException {
    DavResource davResource = new DavResource(httpClient, httpUrl);
    ResponseList responses = new ResponseList();
    davResource.propfind(0, new Name[] {CalendarHomeSet.NAME}, responses);
    Response response = responses.get(0);
    CalendarHomeSet calendarHomeSet = response.get(CalendarHomeSet.class);
    if (calendarHomeSet == null) {
      throw new DisplayableException(R.string.caldav_home_set_not_found);
    }
    List<String> hrefs = calendarHomeSet.getHrefs();
    if (hrefs.size() != 1) {
      throw new DisplayableException(R.string.caldav_home_set_not_found);
    }
    String homeSet = hrefs.get(0);
    if (isEmpty(homeSet)) {
      throw new DisplayableException(R.string.caldav_home_set_not_found);
    }
    return davResource.getLocation().resolve(homeSet).toString();
  }

  String getHomeSet()
      throws IOException, DavException, NoSuchAlgorithmException, KeyManagementException {
    String principal = null;
    try {
      principal = tryFindPrincipal("/.well-known/caldav");
    } catch (Exception e) {
      if (e instanceof HttpException && ((HttpException) e).getCode() == 401) {
        throw e;
      }

      Timber.w(e);
    }
    if (principal == null) {
      principal = tryFindPrincipal("");
    }
    return forUrl(
            (isEmpty(principal) ? this.httpUrl : httpUrl.resolve(principal)).toString(),
            basicDigestAuthHandler.getUsername(),
            basicDigestAuthHandler.getPassword())
        .findHomeset();
  }

  public List<Response> getCalendars() throws IOException, DavException {
    DavResource davResource = new DavResource(httpClient, httpUrl);
    ResponseList responses = new ResponseList(HrefRelation.MEMBER);
    davResource.propfind(
        1,
        new Name[] {
          ResourceType.NAME,
          DisplayName.NAME,
          SupportedCalendarComponentSet.NAME,
          GetCTag.NAME,
          SyncToken.NAME
        },
        responses);
    List<Response> urls = new ArrayList<>();
    for (Response member : responses) {
      ResourceType resourceType = member.get(ResourceType.class);
      if (resourceType == null
          || !resourceType.getTypes().contains(ResourceType.Companion.getCALENDAR())) {
        Timber.d("%s is not a calendar", member);
        continue;
      }
      SupportedCalendarComponentSet supportedCalendarComponentSet =
          member.get(SupportedCalendarComponentSet.class);
      if (supportedCalendarComponentSet == null
          || !supportedCalendarComponentSet.getSupportsTasks()) {
        Timber.d("%s does not support tasks", member);
        continue;
      }
      urls.add(member);
    }
    return urls;
  }

  void deleteCollection() throws IOException, HttpException {
    new DavResource(httpClient, httpUrl).delete(null, response -> null);
  }

  String makeCollection(String displayName)
      throws IOException, XmlPullParserException, HttpException {
    DavResource davResource =
        new DavResource(httpClient, httpUrl.resolve(UUIDHelper.newUUID() + "/"));
    String mkcolString = getMkcolString(displayName);
    davResource.mkCol(mkcolString, response -> null);
    return davResource.getLocation().toString();
  }

  private String getMkcolString(String displayName) throws IOException, XmlPullParserException {
    XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
    XmlSerializer xml = xmlPullParserFactory.newSerializer();
    StringWriter stringWriter = new StringWriter();
    xml.setOutput(stringWriter);
    xml.startDocument("UTF-8", null);
    xml.setPrefix("", NS_WEBDAV);
    xml.setPrefix("CAL", NS_CALDAV);
    xml.setPrefix("CARD", NS_CARDDAV);
    xml.startTag(NS_WEBDAV, "mkcol");
    xml.startTag(XmlUtils.NS_WEBDAV, "set");
    xml.startTag(XmlUtils.NS_WEBDAV, "prop");
    xml.startTag(XmlUtils.NS_WEBDAV, "resourcetype");
    xml.startTag(XmlUtils.NS_WEBDAV, "collection");
    xml.endTag(XmlUtils.NS_WEBDAV, "collection");
    xml.startTag(XmlUtils.NS_CALDAV, "calendar");
    xml.endTag(XmlUtils.NS_CALDAV, "calendar");
    xml.endTag(XmlUtils.NS_WEBDAV, "resourcetype");
    xml.startTag(XmlUtils.NS_WEBDAV, "displayname");
    xml.text(displayName);
    xml.endTag(XmlUtils.NS_WEBDAV, "displayname");
    xml.startTag(XmlUtils.NS_CALDAV, "supported-calendar-component-set");
    xml.startTag(XmlUtils.NS_CALDAV, "comp");
    xml.attribute(null, "name", "VTODO");
    xml.endTag(XmlUtils.NS_CALDAV, "comp");
    xml.endTag(XmlUtils.NS_CALDAV, "supported-calendar-component-set");
    xml.endTag(XmlUtils.NS_WEBDAV, "prop");
    xml.endTag(XmlUtils.NS_WEBDAV, "set");
    xml.endTag(XmlUtils.NS_WEBDAV, "mkcol");
    xml.endDocument();
    xml.flush();
    return stringWriter.toString();
  }

  OkHttpClient getHttpClient() {
    return httpClient;
  }

  CaldavClient setForeground() {
    foreground = true;
    return this;
  }
}
