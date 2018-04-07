package org.tasks.caldav;

import static android.text.TextUtils.isEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import at.bitfire.dav4android.BasicDigestAuthHandler;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.PropertyCollection;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.CalendarHomeSet;
import at.bitfire.dav4android.property.CurrentUserPrincipal;
import at.bitfire.dav4android.property.DisplayName;
import at.bitfire.dav4android.property.GetCTag;
import at.bitfire.dav4android.property.ResourceType;
import at.bitfire.dav4android.property.SupportedCalendarComponentSet;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.tasks.R;
import org.tasks.data.CaldavAccount;
import org.tasks.security.Encryption;
import org.tasks.ui.DisplayableException;
import timber.log.Timber;

class CaldavClient {

  private final DavResource davResource;
  private HttpUrl httpUrl;

  CaldavClient(CaldavAccount caldavAccount, Encryption encryption) {
    this(
        caldavAccount.getUrl(),
        caldavAccount.getUsername(),
        encryption.decrypt(caldavAccount.getPassword()));
  }

  CaldavClient(String url, String username, String password) {
    BasicDigestAuthHandler basicDigestAuthHandler =
        new BasicDigestAuthHandler(null, username, password);
    OkHttpClient httpClient =
        new OkHttpClient()
            .newBuilder()
            .addNetworkInterceptor(basicDigestAuthHandler)
            .authenticator(basicDigestAuthHandler)
            .cookieJar(new MemoryCookieStore())
            .followRedirects(false)
            .followSslRedirects(true)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    URI uri = URI.create(url);
    httpUrl = HttpUrl.get(uri);
    davResource = new DavResource(httpClient, httpUrl);
  }

  private String tryFindPrincipal() throws DavException, IOException, HttpException {
    for (String link : asList("", "/.well-known/caldav")) {
      HttpUrl url = httpUrl.resolve(link);
      Timber.d("Checking for principal: %s", url);
      davResource.setLocation(url);
      try {
        davResource.propfind(0, CurrentUserPrincipal.NAME);
      } catch (HttpException e) {
        switch (e.getStatus()) {
          case 405:
            Timber.w(e);
            break;
          default:
            throw e;
        }
      }
      PropertyCollection properties = davResource.getProperties();
      CurrentUserPrincipal currentUserPrincipal = properties.get(CurrentUserPrincipal.class);
      if (currentUserPrincipal != null) {
        String href = currentUserPrincipal.getHref();
        if (!isEmpty(href)) {
          return href;
        }
      }
    }
    return null;
  }

  private String findHomeset() throws DavException, IOException, HttpException {
    davResource.propfind(0, CalendarHomeSet.NAME);
    PropertyCollection properties = davResource.getProperties();
    CalendarHomeSet calendarHomeSet = properties.get(CalendarHomeSet.class);
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

  public Single<String> getHomeSet() {
    return Single.fromCallable(
            () -> {
              String principal = tryFindPrincipal();
              if (!isEmpty(principal)) {
                davResource.setLocation(httpUrl.resolve(principal));
              }
              return findHomeset();
            })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public List<DavResource> getCalendars() {
    try {
      davResource.propfind(
          1, ResourceType.NAME, DisplayName.NAME, SupportedCalendarComponentSet.NAME, GetCTag.NAME);
    } catch (IOException | HttpException | DavException e) {
      Timber.e(e);
      return emptyList();
    }
    List<DavResource> urls = new ArrayList<>();
    for (DavResource member : davResource.getMembers()) {
      PropertyCollection properties = member.getProperties();
      ResourceType resourceType = properties.get(ResourceType.class);
      if (resourceType == null || !resourceType.getTypes().contains(ResourceType.CALENDAR)) {
        Timber.d("%s is not a calendar", member);
        continue;
      }
      SupportedCalendarComponentSet supportedCalendarComponentSet =
          properties.get(SupportedCalendarComponentSet.class);
      if (supportedCalendarComponentSet == null
          || !supportedCalendarComponentSet.getSupportsTasks()) {
        Timber.d("%s does not support tasks", member);
        continue;
      }
      Timber.d("Found %s", member);
      urls.add(member);
    }
    if (urls.isEmpty()) {
      throw new DisplayableException(R.string.caldav_no_supported_calendars);
    }
    return urls;
  }

  public Single<String> getDisplayName() {
    Callable<String> callable =
        () -> {
          davResource.propfind(0, DisplayName.NAME);
          DisplayName displayName = davResource.getProperties().get(DisplayName.class);
          if (displayName == null) {
            throw new DisplayableException(R.string.calendar_not_found);
          }
          return displayName.getDisplayName();
        };
    return Single.fromCallable(callable)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }
}
