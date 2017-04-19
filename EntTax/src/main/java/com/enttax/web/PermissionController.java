package com.enttax.web;

import com.enttax.model.Staff;
import com.enttax.service.PermissService;
import com.enttax.util.constant.ConstantException;
import com.enttax.util.constant.ConstantStr;
import com.enttax.util.tools.Encodes;
import com.enttax.util.tools.FileUploadUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * 权限模块  包括登录、注册
 * Created by lcyanxi on 17-3-13.
 */
@Controller
@RequestMapping(value = "/user")
public class PermissionController extends BaseController {
    private PermissService permissService;

    @Autowired
    public void setPermissService(PermissService permissService) {
        this.permissService = permissService;
    }


    /**
     * 注册功能  用户名和密码为必须字段0
     *
     * @param staff
     * @return
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@RequestParam("rid") String rid,
                           @ModelAttribute Staff staff, Model model) {
        boolean result = permissService.register(staff, rid);
        if (result) {
            model.addAttribute(ConstantStr.STATUS, ConstantStr.str_one);
//            model.addAttribute("sid",sid);
            return "successful";
        } else {
            model.addAttribute(ConstantStr.STATUS, ConstantStr.str_zero);
            return "error";
        }
    }


    /**
     * 更新个人信息
     *
     * @param staff
     * @return
     */
    @RequestMapping(value = "/updateStaffInfo", method = RequestMethod.POST)
    public String updateStaffInfo(@ModelAttribute Staff staff) {
        if (staff.getSid() == null) {
            return "staffInfo";
        }
        staff.setSpassword(Encodes.encodeBase64(staff.getSpassword()));
        permissService.updateStaffInfo(staff);
        return "staffInfo";
    }


    /**
     * 头像上传
     *
     * @param x
     * @param y
     * @param h
     * @param w
     * @param imageFile
     * @return
     */
    @RequestMapping(value = "/uploadHeadImage", method = RequestMethod.POST)
    public String uploadHeadImage(
            @RequestParam(value = "x") String x,
            @RequestParam(value = "y") String y,
            @RequestParam(value = "h") String h,
            @RequestParam(value = "w") String w,
            @RequestParam(value = "imgFile") MultipartFile imageFile,
            Model model) {
        String realPath = session.getServletContext().getRealPath("/");
        try {
            String savator = FileUploadUtil.uploadHeadImage(realPath, x, y, h, w, imageFile);
            if (savator == null) {
                return "index";
            }

            Staff staff = (Staff) model.asMap().get(ConstantStr.STAFFINFO);
            staff.setSavator(savator);
            permissService.updateStaffInfo(staff);
        } catch (IOException e) {

        }
        return "image";
    }


    /**
     * 重置密码
     *
     * @param spassword
     */
    @RequestMapping(value = "/updatepassword", method = RequestMethod.POST)
    public String updateToPassword(@RequestParam(value = "spassword") String spassword,
                                   @RequestParam(value = "code") String code, Model model) {

        String smsCode = (String) session.getAttribute(ConstantStr.SMSCODE);
        String emailCode = (String) session.getAttribute(ConstantStr.EMAILCODE);

        String resultView = "login";

        if (smsCode != null) {

            resultView = "phonereset";
        } else if (emailCode != null) {

            resultView = "mailreset";
        }

        if (spassword == null || spassword == "") {
            model.addAttribute(ConstantStr.MESSAGE, "密码不能为空！！");
            return resultView;

        } else {
            if (code.equals(smsCode) || code.equals(emailCode)) {

                String sid = (String) session.getAttribute(ConstantStr.SID);

                if (permissService.updateToPassword(sid, Encodes.encodeBase64(spassword))) {
                    model.addAttribute(ConstantStr.MESSAGE, "密码更新成功！！");
                    return "login";
                } else {
                    model.addAttribute(ConstantStr.MESSAGE, "密码更新失败!！");
                }
            } else {
                model.addAttribute(ConstantStr.MESSAGE, "验证码不正确！！");
            }


        }
        return resultView;
    }

    /**
     * 处理登响应
     *
     * @param sname
     * @param spassword
     * @param kcode
     * @param model
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(
            @RequestParam("sname") String sname,
            @RequestParam("spassword") String spassword,
            @RequestParam("kcode") String kcode,
            Model model) {


        Map<String, String> map = new HashMap<String, String>();

        //判断参数是否为空
        if (sname == null || sname.equals("") || spassword == null || spassword.equals("")) {
            model.addAttribute(ConstantStr.STATUS, ConstantException.args_error_code);
            model.addAttribute(ConstantStr.MESSAGE, ConstantException.args_error_message);
            return "login";
        }
        //判断验证码是否正确
        if (!kcode.toLowerCase().equals(request.getSession().getAttribute(ConstantStr.SRAND))) {
            model.addAttribute(ConstantStr.STATUS, ConstantException.image_error_code);
            model.addAttribute(ConstantStr.MESSAGE, ConstantException.image_error_message);
            return "login";
        }

        UsernamePasswordToken token = new UsernamePasswordToken(sname, spassword);
        token.setRememberMe(true);
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
            if (subject.isAuthenticated()) {
                model.addAttribute(ConstantStr.STATUS, ConstantException.sucess_code);
                return "index";
            }
        } catch (IncorrectCredentialsException e) {
            model.addAttribute(ConstantStr.MESSAGE, "登录密码错误");
        } catch (ExcessiveAttemptsException e) {
            model.addAttribute(ConstantStr.MESSAGE, "登录失败次数过多");
        } catch (LockedAccountException e) {
            model.addAttribute(ConstantStr.MESSAGE, "帐号已被锁定");
        } catch (DisabledAccountException e) {
            model.addAttribute(ConstantStr.MESSAGE, "帐号已被禁用");
        } catch (ExpiredCredentialsException e) {
            model.addAttribute(ConstantStr.MESSAGE, "帐号已过期");
        } catch (UnknownAccountException e) {
            model.addAttribute(ConstantStr.MESSAGE, "帐号不存在");
        } catch (UnauthorizedException e) {
            model.addAttribute(ConstantStr.MESSAGE, "您没有得到相应的授权！");
        }

        model.addAttribute(ConstantStr.STATUS, ConstantStr.str_zero);
        return "login";
    }
}