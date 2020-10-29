package cn.aaron911.oauth.request;

import com.alibaba.fastjson.JSONObject;

import cn.aaron911.oauth.cache.AuthStateCache;
import cn.aaron911.oauth.config.AuthConfig;
import cn.aaron911.oauth.config.AuthDefaultSource;
import cn.aaron911.oauth.enums.AuthUserGender;
import cn.aaron911.oauth.exception.AuthException;
import cn.aaron911.oauth.model.AuthCallback;
import cn.aaron911.oauth.model.AuthToken;
import cn.aaron911.oauth.model.AuthUser;
import cn.aaron911.oauth.utils.UrlBuilder;

/**
 * Gitlab登录
 *
 */
public class AuthGitlabRequest extends AuthDefaultRequest {

    public AuthGitlabRequest(AuthConfig config) {
        super(config, AuthDefaultSource.GITLAB);
    }

    public AuthGitlabRequest(AuthConfig config, AuthStateCache authStateCache) {
        super(config, AuthDefaultSource.GITLAB, authStateCache);
    }

    @Override
    protected AuthToken getAccessToken(AuthCallback authCallback) {
        String response = doPostAuthorizationCode(authCallback.getCode());
        JSONObject object = JSONObject.parseObject(response);

        this.checkResponse(object);

        return AuthToken.builder()
            .accessToken(object.getString("access_token"))
            .refreshToken(object.getString("refresh_token"))
            .idToken(object.getString("id_token"))
            .tokenType(object.getString("token_type"))
            .scope(object.getString("scope"))
            .build();
    }

    @Override
    protected AuthUser getUserInfo(AuthToken authToken) {
        String response = doGetUserInfo(authToken);
        JSONObject object = JSONObject.parseObject(response);

        this.checkResponse(object);

        return AuthUser.builder()
            .rawUserInfo(object)
            .uuid(object.getString("id"))
            .username(object.getString("username"))
            .nickname(object.getString("name"))
            .avatar(object.getString("avatar_url"))
            .blog(object.getString("web_url"))
            .company(object.getString("organization"))
            .location(object.getString("location"))
            .email(object.getString("email"))
            .remark(object.getString("bio"))
            .gender(AuthUserGender.UNKNOWN)
            .token(authToken)
            .source(source.toString())
            .build();
    }

    private void checkResponse(JSONObject object) {
        // oauth/token 验证异常
        if (object.containsKey("error")) {
            throw new AuthException(object.getString("error_description"));
        }
        // user 验证异常
        if (object.containsKey("message")) {
            throw new AuthException(object.getString("message"));
        }
    }

    /**
     * 返回带{@code state}参数的授权url，授权回调时会带上这个{@code state}
     *
     * @param state state 验证授权流程的参数，可以防止csrf
     * @return 返回授权地址
     * @since 1.11.0
     */
    @Override
    public String authorize(String state) {
        return UrlBuilder.fromBaseUrl(super.authorize(state))
            .queryParam("scope", "read_user+openid+profile+email")
            .build();
    }

}
