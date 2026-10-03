package com.ridelink.ride.client;


import com.ridelink.ride.exception.ServiceException;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.*;
/** Shared transport only; no external databases, credentials or fallback fixtures. */
public class IntegrationHttp {
    private final RestClient client;
    private final String dependency;
    public IntegrationHttp(String baseUrl,String dependency) {
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(2000);factory.setReadTimeout(3000);
        client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();this.dependency=dependency;
    }
    public <T> T exchange(HttpMethod method,String path,Object body,Class<T> type) {
        try {
            var request=client.method(method).uri(path).headers(headers -> {
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if(auth instanceof JwtAuthenticationToken jwt) headers.setBearerAuth(jwt.getToken().getTokenValue());
            });
            if(body!=null) request.contentType(MediaType.APPLICATION_JSON).body(body);
            T result=request.retrieve().body(type);
            if(result==null) throw ServiceException.invalid(dependency);
            return result;
        } catch(RestClientResponseException ex) {
            if(ex.getStatusCode().value()==401 || ex.getStatusCode().value()==403)
                throw new ServiceException(HttpStatus.BAD_GATEWAY,"DEPENDENCY_AUTH_FAILED",dependency+" rejected the token; check team JWT configuration");
            if(ex.getStatusCode().value()==409)
                throw ServiceException.conflict("DEPENDENCY_CONFLICT",dependency+" reported a conflict; retry after checking its state");
            throw ServiceException.upstream(dependency);
        } catch(RestClientException ex) { throw ServiceException.upstream(dependency); }
    }
}
