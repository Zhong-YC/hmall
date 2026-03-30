package com.hmall.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.util.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Null;
import java.lang.annotation.Annotation;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTool jwtTool;
    private final AuthProperties authProperties;

    private AuthProperties properties;

    private final AntPathMatcher antPathMatcher =new AntPathMatcher();


    /**
     * 1.获取request
     * 2.判断是否需要做登录拦截
     * 3.获取token
     * 4.校验并解析token
     * 5.传递用户信息 -> id
     * 6.放行
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取request
        ServerHttpRequest request = exchange.getRequest();
        //2.判断是否需要做登录拦截
        if(isExclude(request.getPath().toString())){
            //放行
            return chain.filter(exchange);
        }
        //3.获取token
        String token=null;
        List<String> headers =request.getHeaders().get("authorization");
        if (headers != null&& !headers.isEmpty()){
            token=headers.get(0);
        }
        //4.校验并解析token
        Long userId= null;
        try {
            userId=jwtTool.parseToken(token);
        }catch (UnauthorizedException e){
            //拦截，设置响应状态码为401
            ServerHttpResponse response=exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //5.传递用户信息 -> id
        String userInfo = userId.toString();
        ServerWebExchange swe = exchange.mutate()
                .request(builder -> builder.header("user-info",userInfo))
                .build();

        //6.放行
        return chain.filter(swe);
    }


    /**
     * 过滤器的优先级，数值越小优先级越高
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 判断是否需要做登录拦截
     * @param path 请求路径
     * @return true:不需要登录拦截，false:需要登录拦截
     */
    public boolean isExclude(String path){
        for (String pathPatten : authProperties.getExcludePaths()){
            if (antPathMatcher.match(pathPatten,path)){
                return true;
            }
        }
        return false;
    }
}
