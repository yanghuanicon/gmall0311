package com.atguigu.gmall0311.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.UserInfo;
import com.atguigu.gmall0311.passport.config.JwtUtil;
import com.atguigu.gmall0311.service.UserInfoService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;


@Controller
public class PassportController {

    @Value("${token.key}")
    private String signKey;

    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("index")
    public String index(HttpServletRequest request){

        String originUrl = request.getParameter("originUrl");

        request.setAttribute("originUrl",originUrl);
        return "index" ;
    }
    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request, UserInfo userInfo){
        //获取IP地址
        String salt = request.getHeader("x-forwarded-for");
        if(userInfo!=null){
            UserInfo loginInfo = userInfoService.login(userInfo);
            if(loginInfo == null) {
                return "fail" ;
            }else {
                //生成token
                HashMap <String, Object> map = new HashMap <>();
                map.put("userId",loginInfo.getId());
                map.put("nickName",loginInfo.getNickName());
                String token = JwtUtil.encode(signKey, map, salt);
                return token;
            }
        }
        return "fail";
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        //解密token
        Map <String, Object> map = JwtUtil.decode(token, signKey, salt);
        if(map != null && map.size()>0){
            String userId = (String) map.get("userId");
            //调用认证方法
            UserInfo userInfo = userInfoService.verify(userId);
            if(userInfo != null){
                return "success" ;
            }
        }
        return "fail" ;
    }
}
