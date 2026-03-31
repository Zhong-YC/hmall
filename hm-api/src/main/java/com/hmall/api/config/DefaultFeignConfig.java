package com.hmall.api.config;


import com.hmall.common.utils.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {

    /**
     * 1.获取用户信息
     * 2.传递用户信息 -> header
     */
    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Long userId = UserContext.getUser();
                if (userId !=null){
                    template.header("user-info",userId.toString());
                }
            }
        };
    }
}
