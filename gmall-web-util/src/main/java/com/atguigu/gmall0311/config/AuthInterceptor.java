package com.atguigu.gmall0311.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.util.HttpClientUtil;


import io.jsonwebtoken.impl.Base64UrlCodec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    public boolean preHandle(javax.servlet.http.HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // http://item.gmall.com/35.html?newToken=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkF0Z3VpZ3UiLCJ1c2VySWQiOiIxIn0.XzRrXwDhYywUAFn-ICLJ9t3Xwz7RHo1VVwZZGNdKaaQ
        // 表示登录成功之后 获取newToken
        String token = request.getParameter("newToken");
        //当newToken不为空时将其放入cookie中
        if(token != null ){
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }
        //http://item.gmall.com/33.html 当用户访问其他业务模块的时候，此时没有newToken ,但是cookie 有可能存在了token
        if(token == null ) {
            //cookie中可能存有token
            token = CookieUtil.getCookieValue(request,"token",false);
        }
        //从token中获取用户名称
        if(token != null && token.length()>0){
            Map<String , Object> map=getUserMapByToken(token);
            String nickName = (String) map.get("nickName");
            //保存用户名称
            request.setAttribute("nickName",nickName);
        }

        // 判断当前控制器上是否有注解LoginRequire
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 看方法上是否有注解
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (methodAnnotation!=null){
            // 获取到的注解
            // 认证：用户是否登录的认证调用PassPortController中verify 控制器
            // http://passport.atguigu.com/verify?token=xxx&salt=xxx
            // http://passport.atguigu.com/verify
            // 如何获取salt
            String salt = request.getHeader("x-forwarded-for");
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            // 判断执行结果
            if ("success".equals(result)){
                // 保存一下userId
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                // 保存nickName
                request.setAttribute("userId",userId);
                return true;
            }else {
                // 什么情况下必须登录！
                if (methodAnnotation.autoRedirect()){
                    // 必须登录！
                    // http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F35.html
                    // 获取浏览器的url
                    String requestURL  = request.getRequestURL().toString();
                    System.out.println("requestURL:"+requestURL); // http://item.gmall.com/34.html/
                    // 对url 进行转码
                    String encodeURL  = URLEncoder.encode(requestURL, "UTF-8");
                    System.out.println("encodeURL:"+encodeURL); // http%3A%2F%2Fitem.gmall.com%2F35.html

                    // 重定向到登录页面 http://passport.atguigu.com/index
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);

                    return false;
                }
            }

        }
        return true;
    }
    private Map<String,Object> getUserMapByToken(String token) {
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class);
        return map ;
    }


}
