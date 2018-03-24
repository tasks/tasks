package org.tasks.caldav;

import at.bitfire.dav4android.BasicDigestAuthHandler;
import at.bitfire.dav4android.DavCalendar;
import at.bitfire.dav4android.property.DisplayName;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.net.URI;
import java.util.concurrent.Callable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.tasks.R;
import org.tasks.data.CaldavAccount;
import org.tasks.ui.DisplayableException;

public class CaldavClient {

  private final DavCalendar davCalendar;

  public CaldavClient(CaldavAccount caldavAccount, Account localAccount) {
    this(caldavAccount.getUrl(), caldavAccount.getUsername(), localAccount.getPassword());
  }

  public CaldavClient(String url, String username, String password) {
    BasicDigestAuthHandler basicDigestAuthHandler = new BasicDigestAuthHandler(null, username,
        password);
    OkHttpClient httpClient = new OkHttpClient().newBuilder()
        .addNetworkInterceptor(basicDigestAuthHandler)
        .authenticator(basicDigestAuthHandler)
        .cookieJar(new MemoryCookieStore())
        .followRedirects(false)
        .followSslRedirects(false)
        .build();
    URI uri = URI.create(url);
    HttpUrl httpUrl = HttpUrl.get(uri);
    davCalendar = new DavCalendar(httpClient, httpUrl);
  }

  public Single<String> getDisplayName() {
    Callable<String> callable = () -> {
      davCalendar.propfind(0, DisplayName.NAME);
      DisplayName displayName = davCalendar.getProperties().get(DisplayName.class);
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
