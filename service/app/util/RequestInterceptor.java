package util;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.auth.verifier.ManagedTokenValidator;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import play.mvc.Http;

/**
 * Request interceptor responsible to authenticated HTTP requests
 *
 * @author Amit Kumar
 */
public class RequestInterceptor {

  public static List<String> restrictedUriList = null;
  private static ConcurrentHashMap<String, Short> apiHeaderIgnoreMap = new ConcurrentHashMap<>();

  private RequestInterceptor() {}

  static {
    restrictedUriList = new ArrayList<>();
    restrictedUriList.add("/v1/user/update");
    restrictedUriList.add("/v1/note/create");
    restrictedUriList.add("/v1/note/update");
    restrictedUriList.add("/v1/note/search");
    restrictedUriList.add("/v1/note/read");
    restrictedUriList.add("/v1/note/delete");
    restrictedUriList.add("/v1/user/feed");

    // ---------------------------
    short var = 1;
    apiHeaderIgnoreMap.put("/v1/user/create", var);
    apiHeaderIgnoreMap.put("/v2/user/create", var);
    apiHeaderIgnoreMap.put("/v3/user/create", var);
    apiHeaderIgnoreMap.put("/v1/user/signup", var);
    apiHeaderIgnoreMap.put("/v1/org/search", var);
    apiHeaderIgnoreMap.put("/service/health", var);
    apiHeaderIgnoreMap.put("/health", var);
    apiHeaderIgnoreMap.put("/v1/notification/email", var);
    apiHeaderIgnoreMap.put("/v1/data/sync", var);
    apiHeaderIgnoreMap.put("/v1/user/data/encrypt", var);
    apiHeaderIgnoreMap.put("/v1/user/data/decrypt", var);
    apiHeaderIgnoreMap.put("/v1/file/upload", var);
    apiHeaderIgnoreMap.put("/v1/user/forgotpassword", var);
    apiHeaderIgnoreMap.put("/v1/user/login", var);
    apiHeaderIgnoreMap.put("/v1/user/logout", var);
    apiHeaderIgnoreMap.put("/v1/object/read/list", var);
    apiHeaderIgnoreMap.put("/v1/object/read", var);
    apiHeaderIgnoreMap.put("/v1/object/create", var);
    apiHeaderIgnoreMap.put("/v1/object/update", var);
    apiHeaderIgnoreMap.put("/v1/object/delete", var);
    apiHeaderIgnoreMap.put("/v1/object/search", var);
    apiHeaderIgnoreMap.put("/v1/object/metrics", var);
    apiHeaderIgnoreMap.put("/v1/client/register", var);
    apiHeaderIgnoreMap.put("/v1/client/key/read", var);
    apiHeaderIgnoreMap.put("/v1/notification/send", var);
    apiHeaderIgnoreMap.put("/v1/user/getuser", var);
    apiHeaderIgnoreMap.put("/v1/notification/audience", var);
    apiHeaderIgnoreMap.put("/v1/org/preferences/read", var);
    apiHeaderIgnoreMap.put("/v1/org/preferences/create", var);
    apiHeaderIgnoreMap.put("/v1/org/preferences/update", var);
    apiHeaderIgnoreMap.put("/v1/telemetry", var);
    // making badging api's as public access
    apiHeaderIgnoreMap.put("/v1/issuer/create", var);
    apiHeaderIgnoreMap.put("/v1/issuer/read", var);
    apiHeaderIgnoreMap.put("/v1/issuer/list", var);
    apiHeaderIgnoreMap.put("/v1/issuer/delete", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/create", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/read", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/search", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/delete", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/assertion/create", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/assertion/read", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/assertion/search", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/assertion/delete", var);
    // making org read as public access
    apiHeaderIgnoreMap.put("/v1/org/read", var);
    // making location APIs public access
    apiHeaderIgnoreMap.put("/v1/location/create", var);
    apiHeaderIgnoreMap.put("/v1/location/update", var);
    apiHeaderIgnoreMap.put("/v1/location/search", var);
    apiHeaderIgnoreMap.put("/v1/location/delete", var);
    apiHeaderIgnoreMap.put("/v1/otp/generate", var);
    apiHeaderIgnoreMap.put("/v1/otp/verify", var);
    apiHeaderIgnoreMap.put("/v1/user/get/email", var);
    apiHeaderIgnoreMap.put("/v1/user/get/phone", var);
    apiHeaderIgnoreMap.put("/v1/user/get/loginId", var);
    apiHeaderIgnoreMap.put("/v1/user/get/loginid", var);
    apiHeaderIgnoreMap.put("/v1/system/settings/get", var);
    apiHeaderIgnoreMap.put("/v1/system/settings/list", var);
    apiHeaderIgnoreMap.put("/v1/user/mock/read", var);
    apiHeaderIgnoreMap.put("/v1/cache/clear", var);
    apiHeaderIgnoreMap.put("/private/user/v1/search", var);
    apiHeaderIgnoreMap.put("/private/user/v1/migrate", var);
    apiHeaderIgnoreMap.put("/private/user/v1/identifier/freeup", var);
    apiHeaderIgnoreMap.put("/private/user/v1/password/reset", var);
    apiHeaderIgnoreMap.put("/private/user/v1/certs/add", var);
    apiHeaderIgnoreMap.put("/v1/user/exists/email", var);
    apiHeaderIgnoreMap.put("/v1/user/exists/phone", var);
    apiHeaderIgnoreMap.put("/v1/role/read", var);
  }

  private static String getUserRequestedFor(Http.Request request) {
    String requestedForUserID = null;
    JsonNode jsonBody = request.body().asJson();
    if (!(jsonBody == null)) { // for search and update and create_mui api's
      if (!(jsonBody.get(JsonKey.REQUEST).get(JsonKey.USER_ID) == null)) {
        requestedForUserID = jsonBody.get(JsonKey.REQUEST).get(JsonKey.USER_ID).asText();
      }
    } else { // for read-api
      String uuidSegment = null;
      Path path = Paths.get(request.uri());
      if (request.queryString().isEmpty()) {
        uuidSegment = path.getFileName().toString();
      } else {
        String[] queryPath = path.getFileName().toString().split("\\?");
        uuidSegment = queryPath[0];
      }
      try {
        requestedForUserID = UUID.fromString(uuidSegment).toString();
      } catch (IllegalArgumentException iae) {
        ProjectLogger.log("Perhaps this is another API, like search that doesn't carry user id.");
      }
    }
    return requestedForUserID;
  }

  /**
   * Authenticates given HTTP request context
   *
   * @param request HTTP play request
   * @return User or Client ID for authenticated request. For unauthenticated requests, UNAUTHORIZED
   *     is returned release-3.0.0 on-wards validating managedBy token.
   */
  public static String verifyRequestData(Http.Request request) {
    String clientId = JsonKey.UNAUTHORIZED;
    request.flash().put(JsonKey.MANAGED_FOR, null);
    Optional<String> accessToken = request.header(HeaderParam.X_Authenticated_User_Token.getName());
    Optional<String> authClientToken =
        request.header(HeaderParam.X_Authenticated_Client_Token.getName());
    Optional<String> authClientId = request.header(HeaderParam.X_Authenticated_Client_Id.getName());
    if (!isRequestInExcludeList(request.path()) && !isRequestPrivate(request.path())) {
      // The API must be invoked with either access token or client token.
      if (accessToken.isPresent()) {
        clientId = AuthenticationHelper.verifyUserAccesToken(accessToken.get());
        if (!JsonKey.USER_UNAUTH_STATES.contains(clientId)) {
          // Now we have some valid token, next verify if the token is matching the request.
          String requestedForUserID = getUserRequestedFor(request);
          if (StringUtils.isNotEmpty(requestedForUserID) && !requestedForUserID.equals(clientId)) {
            // LUA - MUA user combo, check the 'for' token and its parent, child identifiers
            Optional<String> forTokenHeader =
                request.header(HeaderParam.X_Authenticated_For.getName());
            String managedAccessToken = forTokenHeader.isPresent() ? forTokenHeader.get() : "";
            if (StringUtils.isNotEmpty(managedAccessToken)) {
              String managedFor =
                  ManagedTokenValidator.verify(managedAccessToken, clientId, requestedForUserID);
              if (!JsonKey.USER_UNAUTH_STATES.contains(managedFor)) {
                request.flash().put(JsonKey.MANAGED_FOR, managedFor);
              } else {
                clientId = JsonKey.UNAUTHORIZED;
              }
            }
          } else {
            ProjectLogger.log("Ignoring x-authenticated-for token...", LoggerEnum.INFO.name());
          }
        }
      } else if (authClientToken.isPresent() && authClientId.isPresent()) {
        // Client token is present
        clientId =
            AuthenticationHelper.verifyClientAccessToken(authClientId.get(), authClientToken.get());
        if (!JsonKey.UNAUTHORIZED.equals(clientId)) {
          request.flash().put(JsonKey.AUTH_WITH_MASTER_KEY, Boolean.toString(true));
        }
      }
      return clientId;
    } else {
      if (accessToken.isPresent()) {
        String clientAccessTokenId = null;
        try {
          clientAccessTokenId = AuthenticationHelper.verifyUserAccesToken(accessToken.get());
          if (JsonKey.UNAUTHORIZED.equalsIgnoreCase(clientAccessTokenId)) {
            clientAccessTokenId = null;
          }
        } catch (Exception ex) {
          ProjectLogger.log(ex.getMessage(), ex);
          clientAccessTokenId = null;
        }
        return StringUtils.isNotBlank(clientAccessTokenId)
            ? clientAccessTokenId
            : JsonKey.ANONYMOUS;
      }
      return JsonKey.ANONYMOUS;
    }
  }

  private static boolean isRequestPrivate(String path) {
    return path.contains(JsonKey.PRIVATE);
  }

  /**
   * Checks if request URL is in excluded (i.e. public) URL list or not
   *
   * @param requestUrl Request URL
   * @return True if URL is in excluded (public) URLs. Otherwise, returns false
   */
  public static boolean isRequestInExcludeList(String requestUrl) {
    boolean resp = false;
    if (!StringUtils.isBlank(requestUrl)) {
      if (apiHeaderIgnoreMap.containsKey(requestUrl)) {
        resp = true;
      } else {
        String[] splitPath = requestUrl.split("[/]");
        String urlWithoutPathParam = removeLastValue(splitPath);
        if (apiHeaderIgnoreMap.containsKey(urlWithoutPathParam)) {
          resp = true;
        }
      }
    }
    return resp;
  }

  /**
   * Returns URL without path and query parameters.
   *
   * @param splitPath URL path split on slash (i.e. /)
   * @return URL without path and query parameters
   */
  private static String removeLastValue(String splitPath[]) {

    StringBuilder builder = new StringBuilder();
    if (splitPath != null && splitPath.length > 0) {
      for (int i = 1; i < splitPath.length - 1; i++) {
        builder.append("/" + splitPath[i]);
      }
    }
    return builder.toString();
  }
}
