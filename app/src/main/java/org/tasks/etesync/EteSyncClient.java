package org.tasks.etesync;

import static com.google.common.collect.Lists.partition;
import static com.google.common.collect.Lists.transform;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import at.bitfire.cert4android.CustomCertManager;
import at.bitfire.cert4android.CustomCertManager.CustomHostnameVerifier;
import com.etesync.journalmanager.Constants;
import com.etesync.journalmanager.Crypto.CryptoManager;
import com.etesync.journalmanager.Exceptions;
import com.etesync.journalmanager.Exceptions.HttpException;
import com.etesync.journalmanager.Exceptions.IntegrityException;
import com.etesync.journalmanager.Exceptions.VersionTooNewException;
import com.etesync.journalmanager.JournalAuthenticator;
import com.etesync.journalmanager.JournalEntryManager;
import com.etesync.journalmanager.JournalEntryManager.Entry;
import com.etesync.journalmanager.JournalManager;
import com.etesync.journalmanager.JournalManager.Journal;
import com.etesync.journalmanager.UserInfoManager;
import com.etesync.journalmanager.UserInfoManager.UserInfo;
import com.etesync.journalmanager.model.CollectionInfo;
import com.etesync.journalmanager.model.SyncEntry;
import com.etesync.journalmanager.util.TokenAuthenticator;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.internal.tls.OkHostnameVerifier;
import org.tasks.Callback;
import org.tasks.DebugNetworkInterceptor;
import org.tasks.caldav.MemoryCookieStore;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.security.Encryption;
import timber.log.Timber;

public class EteSyncClient {

  private static final String TYPE_TASKS = "TASKS";
  private static final int MAX_FETCH = 50;
  private static final int MAX_PUSH = 30;

  private final Encryption encryption;
  private final Preferences preferences;
  private final DebugNetworkInterceptor interceptor;
  private final String username;
  private final String token;
  private final String encryptionPassword;
  private final OkHttpClient httpClient;
  private final HttpUrl httpUrl;
  private final Context context;
  private final JournalManager journalManager;
  private boolean foreground;

  @Inject
  public EteSyncClient(
      @ForApplication Context context,
      Encryption encryption,
      Preferences preferences,
      DebugNetworkInterceptor interceptor) {
    this.context = context;
    this.encryption = encryption;
    this.preferences = preferences;
    this.interceptor = interceptor;
    username = null;
    token = null;
    encryptionPassword = null;
    httpClient = null;
    httpUrl = null;
    journalManager = null;
  }

  private EteSyncClient(
      Context context,
      Encryption encryption,
      Preferences preferences,
      DebugNetworkInterceptor interceptor,
      String url,
      String username,
      String encryptionPassword,
      String token,
      boolean foreground)
      throws NoSuchAlgorithmException, KeyManagementException {
    this.context = context;
    this.encryption = encryption;
    this.preferences = preferences;
    this.interceptor = interceptor;
    this.username = username;
    this.encryptionPassword = encryptionPassword;
    this.token = token;

    CustomCertManager customCertManager = new CustomCertManager(context);
    customCertManager.setAppInForeground(foreground);
    CustomHostnameVerifier hostnameVerifier =
        customCertManager.hostnameVerifier(OkHostnameVerifier.INSTANCE);
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {customCertManager}, null);

    Builder builder =
        new OkHttpClient()
            .newBuilder()
            .addNetworkInterceptor(new TokenAuthenticator(null, token))
            .cookieJar(new MemoryCookieStore())
            .followRedirects(false)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.getSocketFactory(), customCertManager)
            .hostnameVerifier(hostnameVerifier)
            .readTimeout(30, TimeUnit.SECONDS);
    if (preferences.isFlipperEnabled()) {
      interceptor.add(builder);
    }
    httpClient = builder.build();
    httpUrl = HttpUrl.parse(url);
    journalManager = new JournalManager(httpClient, httpUrl);
  }

  public EteSyncClient forAccount(CaldavAccount account)
      throws NoSuchAlgorithmException, KeyManagementException {
    return forUrl(
        account.getUrl(),
        account.getUsername(),
        account.getEncryptionPassword(encryption),
        account.getPassword(encryption));
  }

  EteSyncClient forUrl(String url, String username, String encryptionPassword, String token)
      throws KeyManagementException, NoSuchAlgorithmException {
    return new EteSyncClient(
        context,
        encryption,
        preferences,
        interceptor,
        url,
        username,
        encryptionPassword,
        token,
        foreground);
  }

  Pair<UserInfo, String> getInfoAndToken(String password) throws IOException, HttpException {
    JournalAuthenticator journalAuthenticator = new JournalAuthenticator(httpClient, httpUrl);
    String token = journalAuthenticator.getAuthToken(username, password);
    return Pair.create(getUserInfo(), token);
  }

  UserInfo getUserInfo() throws HttpException {
    UserInfoManager userInfoManager = new UserInfoManager(httpClient, httpUrl);
    return userInfoManager.fetch(username);
  }

  CryptoManager getCrypto(Journal journal) throws VersionTooNewException, IntegrityException {
    return new CryptoManager(journal.getVersion(), encryptionPassword, journal.getUid());
  }

  private @Nullable CollectionInfo convertJournalToCollection(Journal journal) {
    try {
      CryptoManager cryptoManager = getCrypto(journal);
      journal.verify(cryptoManager);
      CollectionInfo collection =
          CollectionInfo.Companion.fromJson(journal.getContent(cryptoManager));
      collection.updateFromJournal(journal);
      return collection;
    } catch (IntegrityException | VersionTooNewException e) {
      Timber.e(e);
      return null;
    }
  }

  public Map<Journal, CollectionInfo> getCalendars() throws Exceptions.HttpException {
    Map<Journal, CollectionInfo> result = new HashMap<>();
    for (Journal journal : journalManager.list()) {
      CollectionInfo collection = convertJournalToCollection(journal);
      if (collection != null) {
        if (TYPE_TASKS.equals(collection.getType())) {
          Timber.v("Found %s", collection);
          result.put(journal, collection);
        } else {
          Timber.v("Ignoring %s", collection);
        }
      }
    }
    return result;
  }

  void getSyncEntries(
      Journal journal, CaldavCalendar calendar, Callback<List<Pair<Entry, SyncEntry>>> callback)
      throws IntegrityException, Exceptions.HttpException, VersionTooNewException {
    JournalEntryManager journalEntryManager =
        new JournalEntryManager(httpClient, httpUrl, journal.getUid());
    CryptoManager crypto = getCrypto(journal);
    List<Entry> journalEntries;
    do {
      journalEntries = journalEntryManager.list(crypto, calendar.getCtag(), MAX_FETCH);
      callback.call(
          transform(journalEntries, e -> Pair.create(e, SyncEntry.fromJournalEntry(crypto, e))));
    } while (journalEntries.size() >= MAX_FETCH);
  }

  void pushEntries(Journal journal, List<Entry> entries, String remoteCtag) throws HttpException {
    JournalEntryManager journalEntryManager =
        new JournalEntryManager(httpClient, httpUrl, journal.getUid());
    for (List<Entry> partition : partition(entries, MAX_PUSH)) {
      journalEntryManager.create(partition, remoteCtag);
      remoteCtag = partition.get(partition.size() - 1).getUid();
    }
  }

  EteSyncClient setForeground() {
    foreground = true;
    return this;
  }

  void invalidateToken() {
    try {
        new JournalAuthenticator(httpClient, httpUrl).invalidateAuthToken(token);
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  String makeCollection(String name)
      throws VersionTooNewException, IntegrityException, HttpException {
    String uid = Journal.genUid();
    CollectionInfo collectionInfo = new CollectionInfo();
    collectionInfo.setDisplayName(name);
    collectionInfo.setType(TYPE_TASKS);
    collectionInfo.setUid(uid);
    collectionInfo.setSelected(true);
    CryptoManager crypto = new CryptoManager(collectionInfo.getVersion(), encryptionPassword, uid);
    journalManager.create(new Journal(crypto, collectionInfo.toJson(), uid));
    return uid;
  }

  void deleteCollection(CaldavCalendar calendar) throws HttpException {
    journalManager.delete(Journal.fakeWithUid(calendar.getUrl()));
  }

  void createUserInfo(String derivedKey)
      throws HttpException, VersionTooNewException, IntegrityException, IOException {
    CryptoManager cryptoManager =
        new CryptoManager(Constants.CURRENT_VERSION, derivedKey, "userInfo");
    UserInfo userInfo = UserInfo.generate(cryptoManager, username);
    new UserInfoManager(httpClient, httpUrl).create(userInfo);
  }
}
