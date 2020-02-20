package xyz.ruankun.laughingspork.controller;

import io.swagger.annotations.*;
import org.apache.http.HttpRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;
import xyz.ruankun.laughingspork.entity.*;
import xyz.ruankun.laughingspork.service.SxIdentifyFormService;
import xyz.ruankun.laughingspork.service.SxReportService;
import xyz.ruankun.laughingspork.service.SxStudentService;
import xyz.ruankun.laughingspork.service.SxTeacherService;
import xyz.ruankun.laughingspork.shiro.UserToken;
import xyz.ruankun.laughingspork.util.*;
import xyz.ruankun.laughingspork.util.constant.RespCode;
import xyz.ruankun.laughingspork.vo.ResponseVO;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/user")
@Api(tags = {"用户公用接口"})
@CrossOrigin
public class UserController {

    Logger logger = LoggerFactory.getLogger(UserController.class);


    @Autowired
    SxStudentService sxStudentService;

    @Autowired
    SxReportService sxReportService;

    @Autowired
    SxIdentifyFormService sxIdentifyFormService;

    @Autowired
    SxTeacherService sxTeacherService;

    @ApiOperation(value = "用户登录接口", notes = "account类型与loginType一一对应,严格区分大小写.\n" +
            "account(loginType):    " +
            "学生学号(Student)、校内导师编号(Teacher)、",
            httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "account", value = "123456", required = true, paramType = "query"),
            @ApiImplicitParam(name = "password", value = "123", required = true, paramType = "query"),
            @ApiImplicitParam(name = "loginType", value = "Teacher", required = true, paramType = "query"),
            @ApiImplicitParam(name = "code", value = "验证码", required = true, paramType = "query")
    })
    @PostMapping("/login")
    public ResponseVO login(String account, String password, String loginType, String code) {
        // 如果没有验证码
        if(null == code){
            return ControllerUtil.getFalseResultMsgBySelf("验证码字段不能为空");
        }
        //
        if(!VerifyCodeUtil.verify(code.toLowerCase())){
            return ControllerUtil.getFalseResultMsgBySelf("验证码错误！请刷新重试");
        }else {
            // 判断是否是第一次登陆
            SxStudent sxStudent = null;
            if ((sxStudent = sxStudentService.findByStuNo(account)) != null && (sxStudent.getFirstLogin())){
                //第一次
                return ControllerUtil.getFalseResultMsgBySelf("没有注册(没有初始化密码, 但学生信息在数据库存在)");
            }
            // follow code by NadevXiang
            Subject subject = SecurityUtils.getSubject();
            //  设置Session30分钟过期 30分钟没有交互 Seeion将被删除
            subject.getSession().setTimeout(1800000);
            try {
                subject.login(new UserToken(account, password, loginType));
                HashMap<String, Object> data = new HashMap<>();
                //登录成功返回SessionId
                data.put("Authorization", subject.getSession().getId());

                //判断是否有鉴定表和报告册记录 否则创建
                if (sxReportService.getReportInfo(account) == null) {
                    SxReport sxReport = new SxReport();
                    sxReport.setStuNo(account);
                    sxReportService.saveReport(sxReport);
                }
                if (sxIdentifyFormService.getIdentifyInfo(account) == null) {
                    SxIdentifyForm sxIdentifyForm = new SxIdentifyForm();
                    sxIdentifyForm.setStuNo(account);
                    sxIdentifyFormService.saveIdentifyForm(sxIdentifyForm);
                }


                return ControllerUtil.getSuccessResultBySelf(data);
            } catch (IncorrectCredentialsException e) {
                return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_VALIDATION_ERROR + ":" + e.getMessage());
            } catch (UnknownAccountException e) {
                return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_VALIDATION_ERROR + ":" + e.getMessage());
            } catch (Exception e) {
                logger.error(e.toString());
                return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_UNKNOWN_ERROR + ":" + e.getMessage());
            }
            //fi
        }
    }


    @ApiOperation(value = "用户注销接口")
    @GetMapping("/logout")
    @RequiresAuthentication
    public ResponseVO logout() {
        SecurityUtils.getSubject().logout();
        return ControllerUtil.getSuccessResultBySelf("注销成功");
    }

    @ApiOperation(value = "返回当前填写阶段信息", httpMethod = "GET")
    @GetMapping("/reportStage")
    @RequiresAuthentication
    public ResponseVO nowReportStage() {
        SxStagemanage sxStagemanage = sxStudentService.getNowReportStage();
        return ControllerUtil.getSuccessResultBySelf(sxStagemanage);
    }

    @ApiOperation("生成验证码")
    @GetMapping("/verifycode")
    public void getCode(HttpServletResponse response) throws Exception {
        //利用图片工具生成图片
        //第一个参数是生成的验证码，第二个参数是生成的图片
        Object[] objs = VerifyCodeUtil.createImage();
        //将验证码存入验证码池
        VerifyCodeUtil.setVerifyCode(objs[0].toString().toLowerCase());
        //将图片输出给浏览器
        BufferedImage image = (BufferedImage) objs[1];
        response.setContentType("image/png");
        OutputStream os = response.getOutputStream();
        ImageIO.write(image, "png", os);
    }
    /**
     *  是否是第一次登录, 按照官方要求，第一次登录需要设置一个复杂的密码
     * @param account
     * @return
     */
    @ApiOperation(value = "输入学号或者工号，返回是否是第一次登录", httpMethod = "GET")
    @GetMapping("/loginstatus")
    public ResponseVO isFirstLogin(@RequestParam String account){
        System.out.println("账号：" + account);
        SxStudent sxStudent = null;
        SxTeacher sxTeacher = null;
        try{
            sxStudent = sxStudentService.findByStuNo(account);
            if (null == sxStudent){
                sxTeacher = sxTeacherService.findByTeacherNo(account);
                if(null == sxTeacher){
                    return ControllerUtil.getFalseResultMsgBySelf("老师/学生不存在");
                }else{
                    //老师不为空
                    if (sxTeacher.getFirstLogin()){
                        return ControllerUtil.getDataResult("{\"isFirstLogin\":true}");
                    }
                    return ControllerUtil.getDataResult("{\"isFirstLogin\":false}");
                }
            }else{
                //学生不为空
                if (sxStudent.getFirstLogin()){
                    return ControllerUtil.getDataResult("{\"isFirstLogin\":true}");
                }
                return ControllerUtil.getDataResult("{\"isFirstLogin\":false}");
            }
        }catch (Exception e){
            return ControllerUtil.getFalseResultMsgBySelf(e.getMessage());
        }



    }

    @ApiOperation(value = "注册,只有第一次登录才具有注册功能", httpMethod = "POST")
    @PostMapping("/register")
    public ResponseVO register(@RequestParam String account
            , @RequestParam String password
            , @RequestParam String idcard
            , @RequestParam String loginType
            , @RequestParam String code){
        // 如果没有验证码
        if(null == code){
            return ControllerUtil.getFalseResultMsgBySelf("验证码不能为空");
        }
        //
        if(!VerifyCodeUtil.verify(code.toLowerCase())){
            return ControllerUtil.getFalseResultMsgBySelf("验证码输入错误，请刷新后重试");
        }else {
            //先对密码进行校验,不合法的弱密码无法通过注册
            Map<Boolean, String> rs = EasyPwdValidator.validate(password);
            if (null != rs.get(false)){
                return ControllerUtil.getFalseResultMsgBySelf(rs.get(false));
            }

            if (loginType.equals("Student")){
                // student
                SxStudent sxStudent = sxStudentService.findByStuNo(account);
                if (null != sxStudent){
                    if (!sxStudent.getFirstLogin()){
                        return ControllerUtil.getFalseResultMsgBySelf("该学生已经注册");
                    }else {
                        // 注册
                        return register(idcard, password, sxStudent);
                    }
                }else {
                    return ControllerUtil.getFalseResultMsgBySelf("没有这个学生");
                }
            }else if(loginType.equals("Teacher")){
                // teacher
                SxTeacher sxTeacher = sxTeacherService.findByTeacherNo(account);
                if (!sxTeacher.getFirstLogin()){
                    return ControllerUtil.getFalseResultMsgBySelf("老师早已注册");
                }else {
                    // 注册
                    return register(idcard, password, sxTeacher);
                }
            }else {
                return ControllerUtil.getFalseResultMsgBySelf("注册类型有误");
            }
        }
    }

    private <T> ResponseVO register(String idcard,String password, T t){
        if (t instanceof SxStudent){
            //student
            if (!((SxStudent) t).getIdCard().equals(idcard)){
                return ControllerUtil.getFalseResultMsgBySelf("身份证号码填写错误!");
            }
            ((SxStudent) t).setPassword(MD5Util.trueMd5(password).toUpperCase());
            ((SxStudent) t).setFirstLogin(false);   // 注册了就不是第一次登陆了
            try {
                sxStudentService.save((SxStudent) t);
                return ControllerUtil.getSuccessResultBySelf("注册成功,请登录");
            } catch (Exception e) {
                e.printStackTrace();
                return ControllerUtil.getFalseResultMsgBySelf("尝试保存学生信息,但发生以下错误:" + e.getMessage());
            }
        }else if(t instanceof  SxTeacher){
            if (!((SxTeacher) t).getIdCard().equals(idcard)){
                return ControllerUtil.getFalseResultMsgBySelf("身份证号有误!");
            }
            ((SxTeacher) t).setPassword(MD5Util.trueMd5(password).toUpperCase());
            ((SxTeacher) t).setFirstLogin(false);   // 注册了就不是第一次登陆了
            try {
                sxTeacherService.save((SxTeacher) t);
                ((SxTeacher) t).setFirstLogin(false);   // 注册了就不是第一次登陆了
                return ControllerUtil.getSuccessResultBySelf("注册成功,请登录");
            } catch (Exception e) {
                e.printStackTrace();
                return ControllerUtil.getFalseResultMsgBySelf("尝试保存教师信息,但发生以下错误:" + e.getMessage());
            }
        }else{
            return ControllerUtil.getFalseResultMsgBySelf("服务器错误:输入类型既不是学生也不是教师.");
        }
    }

    @PostMapping("/password")
    @ApiOperation("修改密码")
    public ResponseVO changePassword(String account, String type, String idCard,String password){
        Map<Boolean, String> rs = EasyPwdValidator.validate(password);
        if (null != rs.get(false)){
            return ControllerUtil.getFalseResultMsgBySelf(rs.get(false));
        }
        if (type.equals("Teacher")){
            SxTeacher sxTeacher = sxTeacherService.findByTeacherNo(account);
            if (null != sxTeacher && !idCard.equals(sxTeacher.getIdCard())){
                return ControllerUtil.getFalseResultMsgBySelf("身份证号码错误");
            }else if(null != sxTeacher) {
                sxTeacher.setPassword(MD5Util.trueMd5(password).toUpperCase());
                sxTeacherService.save(sxTeacher);
                return ControllerUtil.getTrueOrFalseResult(true);
            }else{
                return ControllerUtil.getFalseResultMsgBySelf("未查询到该教师信息,账号：" + account);
            }
        }else if(type.equals("Student")){
            SxStudent sxStudent = sxStudentService.findByStuNo(account);
            if (null != sxStudent && !idCard.equals(sxStudent.getIdCard())){
                return ControllerUtil.getFalseResultMsgBySelf("身份证号码错误");
            }else if(null != sxStudent){
                sxStudent.setPassword(MD5Util.trueMd5(password).toUpperCase());
                sxStudent.setFirstLogin(false);
                sxStudentService.save(sxStudent);
                return ControllerUtil.getTrueOrFalseResult(true);
            }else{
                return ControllerUtil.getFalseResultMsgBySelf("未查询到该学生信息,账号：" + account);
            }
        }else {
            return ControllerUtil.getFalseResultMsgBySelf("用户身份类型不符合");
        }
    }

}
