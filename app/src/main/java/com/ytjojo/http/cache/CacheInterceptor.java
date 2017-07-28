package com.ytjojo.http.cache;

import java.io.IOException;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Internal;
import okhttp3.internal.Util;
import okhttp3.internal.cache.CacheRequest;
import okhttp3.internal.cache.CacheStrategy;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import okio.Source;
import okio.Timeout;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static okhttp3.internal.Util.closeQuietly;
import static okhttp3.internal.Util.discard;

/** Serves requests from the cache and writes responses to the cache. */
public final class CacheInterceptor implements Interceptor {
  public static final String HEADER_DYNAMIC_KEY="CHACHE_DYNAMIC_KEY";
  public static final String HEADER_DYNAMIC_KEY_GROUP  = "CHACHE_DYNAMIC_KEY_GROUP";
  public static final String HEADER_CACHE_TIME  = "CACHEINTERCEPTOR_CACHE_TIME";

	final Cache cache;
  private boolean forceKey=true;

  public CacheInterceptor(Cache cache) {
    this.cache = cache;
  }
  public CacheInterceptor(Cache cache,boolean forceKey) {
    this.cache = cache;
    this.forceKey = forceKey;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if(!request.method().equals("POST")){
      return chain.proceed(request);
    }
    String dynamicKey = request.header(HEADER_DYNAMIC_KEY);
    String dynamicKeyGroup = request.header(HEADER_DYNAMIC_KEY_GROUP);
    String cachetTime = request.header(HEADER_CACHE_TIME);
    if(forceKey &&(dynamicKeyGroup ==null && dynamicKey ==null)){
      return chain.proceed(request);
    }
    Response cacheCandidate = cache != null
        ? cache.get(chain.request(),dynamicKey,dynamicKeyGroup)
        : null;

    long now = System.currentTimeMillis();

    CacheStrategy strategy = new CacheStrategy.Factory(now, chain.request(), cacheCandidate).get();
    Request networkRequest = strategy.networkRequest;
    Response cacheResponse = strategy.cacheResponse;

    if (cache != null) {
      cache.trackResponse(strategy);
    }

    if (cacheCandidate != null && cacheResponse == null) {
      closeQuietly(cacheCandidate.body()); // The cache candidate wasn't applicable. Close it.
    }

    // If we're forbidden from using the network and the cache is insufficient, fail.
    if (networkRequest == null && cacheResponse == null) {
      return new Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(504)
          .message("Unsatisfiable Request (only-if-cached)")
          .body(Util.EMPTY_RESPONSE)
          .sentRequestAtMillis(-1L)
          .receivedResponseAtMillis(System.currentTimeMillis())
          .build();
    }

    // If we don't need the network, we're done.
    if (networkRequest == null) {
      return cacheResponse.newBuilder()
          .cacheResponse(stripBody(cacheResponse))
          .build();
    }

    Response networkResponse = null;
    try {
      networkResponse = chain.proceed(networkRequest);
      String cacheControlValue= cachetTime==null? String.format("max-age=%s", cachetTime):"public, only-if-cached, max-stale= 2147483647";
      networkResponse = networkResponse.newBuilder()
          .header("Cache-Control", cacheControlValue)
          .removeHeader("Pragma")
          .networkResponse(null)
          .build();
    } finally {
      // If we're crashing on I/O or otherwise, don't leak the cache body.
      if (networkResponse == null && cacheCandidate != null) {
        closeQuietly(cacheCandidate.body());
      }
    }

    // If we have a cache response too, then we're doing a conditional get.
    if (cacheResponse != null) {
      if (networkResponse.code() == HTTP_NOT_MODIFIED) {
        Response response = cacheResponse.newBuilder()
            .headers(combine(cacheResponse.headers(), networkResponse.headers()))
            .sentRequestAtMillis(networkResponse.sentRequestAtMillis())
            .receivedResponseAtMillis(networkResponse.receivedResponseAtMillis())
            .cacheResponse(stripBody(cacheResponse))
            .networkResponse(stripBody(networkResponse))
            .build();
        networkResponse.body().close();

        // Update the cache after combining headers but before stripping the
        // Content-Encoding header (as performed by initContentStream()).
        cache.trackConditionalCacheHit();
        cache.update(cacheResponse, response);
        return response;
      } else {
        closeQuietly(cacheResponse.body());
      }
    }

    Response response = networkResponse.newBuilder()
        .cacheResponse(stripBody(cacheResponse))
        .networkResponse(stripBody(networkResponse))
        .build();

    if (HttpHeaders.hasBody(response)) {
      CacheRequest cacheRequest = maybeCache(response, networkResponse.request(), cache,dynamicKey,dynamicKeyGroup);
      response = cacheWritingResponse(cacheRequest, response);
    }

    return response;
  }

  private static Response stripBody(Response response) {
    return response != null && response.body() != null
        ? response.newBuilder().body(null).build()
        : response;
  }

  private CacheRequest maybeCache(Response userResponse, Request networkRequest,
      Cache responseCache, String dynamicKey,String dynamicKeyGroup) throws IOException {
    if (responseCache == null) return null;

    // Should we cache this response for this request?
    if (!CacheStrategy.isCacheable(userResponse, networkRequest)) {
      if (Cache.invalidatesCache(networkRequest.method())) {
        try {
          responseCache.remove(networkRequest,dynamicKey,dynamicKeyGroup);
        } catch (IOException ignored) {
          // The cache cannot be written.
        }
      }
      return null;
    }

    // Offer this request to the cache.
    return responseCache.put(userResponse,dynamicKey,dynamicKeyGroup);
  }

  /**
   * Returns a new source that writes bytes to {@code cacheRequest} as they are read by the source
   * consumer. This is careful to discard bytes left over when the stream is closed; otherwise we
   * may never exhaust the source stream and therefore not complete the cached response.
   */
  private Response cacheWritingResponse(final CacheRequest cacheRequest, Response response)
      throws IOException {
    // Some apps return a null body; for compatibility we treat that like a null cache request.
    if (cacheRequest == null) return response;
    Sink cacheBodyUnbuffered = cacheRequest.body();
    if (cacheBodyUnbuffered == null) return response;

    final BufferedSource source = response.body().source();
    final BufferedSink cacheBody = Okio.buffer(cacheBodyUnbuffered);

    Source cacheWritingSource = new Source() {
      boolean cacheRequestClosed;

      @Override public long read(Buffer sink, long byteCount) throws IOException {
        long bytesRead;
        try {
          bytesRead = source.read(sink, byteCount);
        } catch (IOException e) {
          if (!cacheRequestClosed) {
            cacheRequestClosed = true;
            cacheRequest.abort(); // Failed to write a complete cache response.
          }
          throw e;
        }

        if (bytesRead == -1) {
          if (!cacheRequestClosed) {
            cacheRequestClosed = true;
            cacheBody.close(); // The cache response is complete!
          }
          return -1;
        }

        sink.copyTo(cacheBody.buffer(), sink.size() - bytesRead, bytesRead);
        cacheBody.emitCompleteSegments();
        return bytesRead;
      }

      @Override public Timeout timeout() {
        return source.timeout();
      }

      @Override public void close() throws IOException {
        if (!cacheRequestClosed
            && !discard(this, HttpCodec.DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)) {
          cacheRequestClosed = true;
          cacheRequest.abort();
        }
        source.close();
      }
    };

    return response.newBuilder()
        .body(new RealResponseBody(response.headers(), Okio.buffer(cacheWritingSource)))
        .build();
  }

  /** Combines cached headers with a network headers as defined by RFC 2616, 13.5.3. */
  private static Headers combine(Headers cachedHeaders, Headers networkHeaders) {
    Headers.Builder result = new Headers.Builder();

    for (int i = 0, size = cachedHeaders.size(); i < size; i++) {
      String fieldName = cachedHeaders.name(i);
      String value = cachedHeaders.value(i);
      if ("Warning".equalsIgnoreCase(fieldName) && value.startsWith("1")) {
        continue; // Drop 100-level freshness warnings.
      }
      if (!isEndToEnd(fieldName) || networkHeaders.get(fieldName) == null) {
        Internal.instance.addLenient(result, fieldName, value);
      }
    }

    for (int i = 0, size = networkHeaders.size(); i < size; i++) {
      String fieldName = networkHeaders.name(i);
      if ("Content-Length".equalsIgnoreCase(fieldName)) {
        continue; // Ignore content-length headers of validating responses.
      }
      if (isEndToEnd(fieldName)) {
        Internal.instance.addLenient(result, fieldName, networkHeaders.value(i));
      }
    }

    return result.build();
  }

  /**
   * Returns true if {@code fieldName} is an end-to-end HTTP header, as defined by RFC 2616,
   * 13.5.1.
   */
  static boolean isEndToEnd(String fieldName) {
    return !"Connection".equalsIgnoreCase(fieldName)
        && !"Keep-Alive".equalsIgnoreCase(fieldName)
        && !"Proxy-Authenticate".equalsIgnoreCase(fieldName)
        && !"Proxy-Authorization".equalsIgnoreCase(fieldName)
        && !"TE".equalsIgnoreCase(fieldName)
        && !"Trailers".equalsIgnoreCase(fieldName)
        && !"Transfer-Encoding".equalsIgnoreCase(fieldName)
        && !"Upgrade".equalsIgnoreCase(fieldName);
  }
  static Request getCachedRequest(){
    Request request = new Request.Builder()
        .url("htt://com.githug.ytjojo/getCache")
        .method("GET",null)
        .cacheControl(CacheControl.FORCE_CACHE)
        .build();
    return request;
  }
  static Response getCachedResponse(Request request,long now){
    return new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(Util.EMPTY_RESPONSE)
        .sentRequestAtMillis(now)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build();

  }
  static void putToCache(Cache cache,Request request,long now){
    Response cacheCandidate = cache != null
        ? cache.get(request)
        : null;
    if(cacheCandidate ==null && cache !=null){
      Response response= getCachedResponse(request,now);
      cache.put(response);
    }
  }
  static void proceed(Chain chain,long now,Cache cache){
    Request request = getCachedRequest();
    putToCache(cache,request,now);
    try {
      chain.proceed(request);
    }catch (Exception e){

    }

  }

}
