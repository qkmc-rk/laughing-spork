package xyz.ruankun.laughingspork.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jodconverter.DocumentConverter;
import org.jodconverter.office.LocalOfficeManager;
import org.jodconverter.office.OfficeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.ruankun.laughingspork.entity.*;
import xyz.ruankun.laughingspork.service.*;
import xyz.ruankun.laughingspork.util.*;
import xyz.ruankun.laughingspork.util.constant.RespCode;
import xyz.ruankun.laughingspork.util.constant.RoleCode;
import xyz.ruankun.laughingspork.vo.ResponseVO;

import java.io.File;
import java.io.OutputStream;
import java.sql.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@CrossOrigin
@RequestMapping("/student")
@Api(tags = {"学生操作"})
public class StudentController {
    public static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    SxStudentService sxStudentService;

    @Autowired
    SxIdentifyFormService sxIdentifyFormService;

    @Autowired
    SxReportService sxReportService;

    @Autowired
    SxTeacherService sxTeacherService;

    @Autowired
    SxCorporationService sxCorporationService;

    @Autowired
    DocumentConverter documentConverter;


    @Value(value = "${user.password}")
    private String psd;


    @ApiOperation(value = "返回当前报告册阶段信息", httpMethod = "GET")
    @GetMapping("/reportStage")
    @RequiresRoles(RoleCode.STUDENT)
    public ResponseVO nowReportStage() {
        SxStagemanage sxStagemanage = sxStudentService.getNowReportStage();
        return ControllerUtil.getSuccessResultBySelf(sxStagemanage);

    }


    @ApiOperation(value = "学生查看自己信息", httpMethod = "GET")
    @RequiresRoles(RoleCode.STUDENT)
    @GetMapping("/selfInfo")
    public ResponseVO getSelfInfo() {
        SxStudent sxStudent  = (SxStudent) SecurityUtils.getSubject().getPrincipal();
        SxReport sxReport = sxReportService.getReportInfo(sxStudent.getStuNo());
        sxStudent.setGmtEnd(sxReport.getGmtEnd());
        sxStudent.setGmtStart(sxReport.getGmtStart());
        return ControllerUtil.getDataResult(sxStudent);
    }

    @ApiOperation(value = "学生修改自己信息", httpMethod = "POST")
    @RequiresRoles(RoleCode.STUDENT)
    @PostMapping("/selfInfo")
    public ResponseVO changeSelfInfo(@RequestParam String phone, @RequestParam String wechat
            , @RequestParam String qq, @RequestParam Integer age, @RequestParam String corpTeacherNo) {

        SxStudent sxStudent  = (SxStudent) SecurityUtils.getSubject().getPrincipal();
        SxStudent sxStudent1 = new SxStudent();
        sxStudent1.setPhone(phone);
        sxStudent1.setWechat(wechat);
        sxStudent1.setQq(qq);
        sxStudent1.setAge(age);
        sxStudent1.setCorpTeacherNo(corpTeacherNo);
        EntityUtil.update(sxStudent1, sxStudent);
        logger.info("sxStudent未更新前: " + sxStudent.toString());
        logger.info("sxStudent即将被更新: " + sxStudent1.toString());
        try{
            sxStudentService.save(sxStudent1);
            return ControllerUtil.getSuccessResultBySelf(sxStudent1);
        }catch (Exception e){
            return ControllerUtil.getFalseResultMsgBySelf("更新出错：" + e.getMessage());
        }
    }

    @ApiOperation(value = "学生查看自己校内导师信息", httpMethod = "GET")
    @RequiresRoles(RoleCode.STUDENT)
    @GetMapping("/teacherInfo")
    public ResponseVO getTeacherInfo() {
        SxTeacher sxTeacher = sxStudentService.getTeacherInfo((SxStudent) SecurityUtils.getSubject().getPrincipal());
        if (sxTeacher == null) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        return ControllerUtil.getDataResult(sxTeacher);
    }

    @ApiOperation(value = "学生查看自己报告册信息", httpMethod = "GET")
    @RequiresRoles(RoleCode.STUDENT)
    @GetMapping("/reportForm")
    public ResponseVO getSelfReportInfo() {
        SxReport sxReport = sxStudentService.getSelfReportInfo((SxStudent) SecurityUtils.getSubject().getPrincipal());
        if (sxReport == null) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        return ControllerUtil.getDataResult(sxReport);
    }


    @ApiOperation(value = "学生查看鉴定表信息", httpMethod = "GET")
    @RequiresRoles(RoleCode.STUDENT)
    @GetMapping("/identifyForm")
    public ResponseVO getSelfIndentifyInfo() {
        SxIdentifyForm sxIdentifyForm = sxStudentService.getSelfIdentifyInfo((SxStudent) SecurityUtils.getSubject().getPrincipal());
        if (sxIdentifyForm == null) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        return ControllerUtil.getDataResult(sxIdentifyForm);
    }


    @ApiOperation(value = "学生填写鉴定表信息", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "practiceContent", value = "实习内容", required = true),
            @ApiImplicitParam(name = "selfSummary", value = "自我总结", required = true),
    })
    @RequiresRoles(RoleCode.STUDENT)
    @PostMapping("/identify")
    public ResponseVO fillIdentifyForm(String practiceContent, String selfSummary,String corpOpinion,String corpTeacherOpinion) {
        if (practiceContent == null || selfSummary == null) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_INCOMPLETE_DATA);
        } else {
            SxStudent sxStudent = (SxStudent) SecurityUtils.getSubject().getPrincipal();
            sxTeacherService.isIdentifyFilledFlag(sxStudent);
            sxTeacherService.isReportFilledFlag(sxStudent);
            sxTeacherService.isIdentifyFlag(sxStudent);
            sxTeacherService.isReportFlag(sxStudent);
            //保存鉴定表内容到数据库
            return ControllerUtil.getDataResult(sxStudentService.saveIdentifyForm(sxStudent, practiceContent, selfSummary,corpOpinion, corpTeacherOpinion));
        }
    }

    @ApiOperation(value = "学生填写报告第一阶段信息", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "stage1date", value = "第一阶段填写时间", required = true),
            @ApiImplicitParam(name = "gmtStart", value = "实习开始时间", required = true),
            @ApiImplicitParam(name = "stage1Summary", value = "自我总结", required = true),
            @ApiImplicitParam(name = "stage1GuideWay", value = "指导方式", required = true),
            @ApiImplicitParam(name = "stage1GuideDate", value = "指导时间,为字符串，可能是一个时间段", required = true),

    })
    @RequiresRoles(RoleCode.STUDENT)
    @PostMapping("/report/stage1")
    public ResponseVO stage1Summary(Date stage1date, Date gmtStart, String stage1Summary, String stage1GuideWay, String stage1GuideDate) {
        if (stage1Summary == null || stage1date == null) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_INCOMPLETE_DATA);
        } else {
            //保存鉴定表内容到数据库
            SxReport sxReport = sxStudentService.setStage1Summary(stage1date,gmtStart,(SxStudent) SecurityUtils.getSubject().getPrincipal(), stage1Summary, stage1GuideWay, stage1GuideDate);
            if (sxReport == null) {
                return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_STAGE_ERROR);
            }
            return ControllerUtil.getDataResult(sxReport);
        }
    }

    @ApiOperation(value = "学生填写报告第二阶段信息", httpMethod = "POST")
    @ApiImplicitParams({

            @ApiImplicitParam(name = "gmtEnd", value = "实习结束时间", required = true),
            @ApiImplicitParam(name = "stage2date", value = "第二阶段填写时间", required = true),
            @ApiImplicitParam(name = "stage2Summary", value = "自我总结", required = true),
            @ApiImplicitParam(name = "stage2GuideWay", value = "指导方式", required = true),
            @ApiImplicitParam(name = "stage2GuideDate", value = "指导时间,为字符串，可能是一个时间段", required = true),

    })
    @RequiresRoles(RoleCode.STUDENT)
    @PostMapping("/report/stage2")
    public ResponseVO stage2Summary(Date stage2Date,Date gmtEnd, String stage2Summary, String stage2GuideWay, String stage2GuideDate) {
        if (stage2Summary == null/* || gmtEnd == null*/) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_INCOMPLETE_DATA);
        } else {
            SxStudent sxStudent = (SxStudent) SecurityUtils.getSubject().getPrincipal();
            sxTeacherService.isIdentifyFilledFlag(sxStudent);
            sxTeacherService.isReportFilledFlag(sxStudent);
            sxTeacherService.isIdentifyFlag(sxStudent);
            sxTeacherService.isReportFlag(sxStudent);
            //保存鉴定表内容到数据库
            SxReport sxReport = sxStudentService.setStage2Summary(stage2Date,gmtEnd,sxStudent, stage2Summary, stage2GuideWay, stage2GuideDate);
            if (sxReport == null) {
                return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_STAGE_ERROR);
            }
            return ControllerUtil.getDataResult(sxReport);
        }
    }

    @ApiOperation(value = "下载学生实习鉴定表", httpMethod = "GET")
    @GetMapping("/identify/form")
    @RequiresRoles(RoleCode.STUDENT)
    public ResponseVO downloadIdentify() throws OfficeException {
        SxStudent sxStudent = (SxStudent) SecurityUtils.getSubject().getPrincipal();
        SxIdentifyForm sxIdentifyForm = sxIdentifyFormService.getIdentifyInfo(sxStudent.getStuNo());

        EntityUtil.setNullFiledToString(sxStudent);
        EntityUtil.setNullFiledToString(sxIdentifyForm);
        Map<String, String> params = new HashMap<>();
        params.put("${college}", sxStudent.getCollege());
        params.put("${major}", sxStudent.getMajor());
        params.put("${stu_name}", sxStudent.getName());
        params.put("${stu_no}", sxStudent.getStuNo());
        params.put("${corp_name}", sxStudent.getCorpName());
        params.put("${gmt_start}", DateUtil.getUpperDate(sxIdentifyForm.getGmtStart()));
        params.put("${gmt_end}", DateUtil.getUpperDate(sxIdentifyForm.getGmtEnd()));
        params.put("${fill_date}", DateUtil.getNowUpperDate());
        params.put("${content}", sxIdentifyForm.getSxContent());
        params.put("${self_summary}", sxIdentifyForm.getSelfSummary());
        params.put("${corp_teacher_opinion}", sxIdentifyForm.getCorpTeacherOpinion());
        params.put("${corp_opinion}", sxIdentifyForm.getCorpOpinion());
        params.put("${teacher_grade}", sxIdentifyForm.getTeacherGrade());
        params.put("${comprehsv_grade}", sxIdentifyForm.getComprehsvGrade());
        params.put("${college_principal_opinion}", sxIdentifyForm.getCollegePrincipalOpinion());
        params.put("${corp_teacher_score}", sxIdentifyForm.getCorpTeacherScore());
        String wordFileName = RenderWordUtil.exportWordToResponse("identify", sxStudent.getStuNo(), params);
        if (wordFileName != null) {
            String path = System.getProperty("user.dir") + (SystemUtil.isWindows()?"\\static\\":"/static/");
            String pdfFileName = wordFileName.replace("docx", "pdf");
            // 源文件 （office）
            File source = new File(path + wordFileName);
            // 目标文件 （pdf）

            File target = new File(path + pdfFileName);
            // 转换文件
            if (!target.exists()) {
                documentConverter.convert(source).to(target).execute();
                //删除word
                source.delete();
            }
            return ControllerUtil.getSuccessResultBySelf(pdfFileName);
        }
        return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_SERVER_ERROR);
    }


    @ApiOperation(value = "下载学生实习报告册", httpMethod = "GET")
    @GetMapping("/report/form")
    @RequiresRoles(RoleCode.STUDENT)
    public ResponseVO downloadReport() throws OfficeException {
        SxStudent sxStudent = (SxStudent) SecurityUtils.getSubject().getPrincipal();
        SxReport sxReport = sxReportService.getReportInfo(sxStudent.getStuNo());
        SxTeacher sxTeacher = sxTeacherService.findByTeacherNo(sxStudent.getTeacherNo());
        EntityUtil.setNullFiledToString(sxStudent);
        EntityUtil.setNullFiledToString(sxReport);
        EntityUtil.setNullFiledToString(sxTeacher);
        Map<String, String> params = new HashMap<>();
        params.put("${stu_name}", sxStudent.getName());
        params.put("${stu_no}", sxStudent.getStuNo());
        params.put("${college}", sxStudent.getCollege());
        params.put("${major}", sxStudent.getMajor());
        params.put("${corp_name}", sxStudent.getCorpName());
        params.put("${corp_position}", sxStudent.getCorpPosition());
        params.put("${stage1_guide_date}", sxReport.getStage1GuideDate());
        params.put("${stage1_guide_way}", sxReport.getStage1GuideWay());
        params.put("${stage1_summary}", sxReport.getStage1Summary());
        params.put("${stage1_comment}", sxReport.getStage1Comment());
        params.put("${stage1_grade}", sxReport.getStage1Grade());
        params.put("${stage2_guide_date}", sxReport.getStage2GuideDate());
        params.put("${stage2_guide_way}", sxReport.getStage2GuideWay());
        params.put("${stage2_summary}", sxReport.getStage2Summary());
        params.put("${stage2_comment}", sxReport.getStage2Comment());
        params.put("${stage2_grade}", sxReport.getStage2Grade());
        params.put("${teacher}", sxTeacher.getName());
        params.put("${total_grade}", sxReport.getTotalGrade());
        params.put("${total_score}", sxReport.getTotalScore());
        params.put("${gmt_start}", DateUtil.getUpperDate(sxReport.getGmtStart()));
        params.put("${gmt_end}", DateUtil.getUpperDate(sxReport.getGmtEnd()));
        params.put("${fill_date}", DateUtil.getNowUpperDate());
        String wordFileName = RenderWordUtil.exportWordToResponse("report", sxStudent.getStuNo(), params);
        if (wordFileName != null) {

            String path = System.getProperty("user.dir") + (SystemUtil.isWindows()?"\\static\\":"/static/");
            String pdfFileName = wordFileName.replace("docx", "pdf");
            // 源文件 （office）
            File source = new File(path + wordFileName);
            // 目标文件 （pdf）

            File target = new File(path + pdfFileName);
            // 转换文件
            if (!target.exists()) {
                documentConverter.convert(source).to(target).execute();
                //删除word
                source.delete();
            }
            return ControllerUtil.getSuccessResultBySelf(pdfFileName);
        }
        return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_SERVER_ERROR);
    }

    @ApiOperation(value = "返回学生所在学院的教师信息", httpMethod = "GET")
    @GetMapping("/teacher")
    public ResponseVO getCollegeTeacher() {
        List<SxTeacher> sxTeachers = sxStudentService.collegeTeacher((SxStudent) SecurityUtils.getSubject().getPrincipal());
        if (sxTeachers == null) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
        }
        return ControllerUtil.getDataResult(sxTeachers);
    }

    @ApiOperation(value = "学生选择自己导师信息", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tNo", value = "教师编号", required = true),
    })
    @PostMapping("/teacher")
    @RequiresRoles(RoleCode.STUDENT)
    public ResponseVO choseTeacher(@RequestParam String tNo) {
        if (tNo == null) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_INCOMPLETE_DATA);
        }
        SxStudent sxStudent = sxStudentService.choseCollegeTeacher((SxStudent) SecurityUtils.getSubject().getPrincipal(), tNo);
        return ControllerUtil.getDataResult(sxStudent);
    }

    @ApiOperation(value = "学生添加/修改企业信息", httpMethod = "POST")
    @RequiresRoles(RoleCode.STUDENT)
    @PostMapping("/student/corp")
    @Transactional
    public ResponseVO addCorpInfo(SxCorporation sxCorporation) {
        //如果该企业已经被核准则不能再修改
        SxStudent sxStudent = (SxStudent) SecurityUtils.getSubject().getPrincipal();
        SxCorporation oldCorp = sxCorporationService.findByStuNo(sxStudent.getStuNo());
        if (null != oldCorp && oldCorp.getIsCorpChecked()){
            return ControllerUtil.getFalseResultMsgBySelf("该企业信息已经被核准，无法继续修改!");
        }

        if (null != oldCorp) {
            if (sxCorporation.getId() == null || sxCorporation.getId().equals("")) {
                return ControllerUtil.getFalseResultMsgBySelf("修改企业信息缺少ID");
            }
            if (oldCorp.getId() != sxCorporation.getId()) {
                return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_NOT_FOUND_DATA);
            }
            EntityUtil.update(sxCorporation,oldCorp);
            System.out.println(oldCorp.toString());
            System.out.println(sxCorporation.toString());
        }

        sxCorporation.setStuNo(sxStudent.getStuNo());
        sxCorporation.setIsCorpChecked(false);
        sxCorporationService.delete(sxCorporation);
        sxCorporation.setId(null);
        sxCorporationService.save(sxCorporation);
        return ControllerUtil.getSuccessResultBySelf(sxCorporationService.findByStuNo(sxStudent.getStuNo()));
    }


    @ApiOperation(value = "学生查看企业信息", httpMethod = "GET")
    @RequiresRoles(RoleCode.STUDENT)
    @GetMapping("/student/corp")
    public ResponseVO addCorpInfo() {
        SxStudent sxStudent = (SxStudent) SecurityUtils.getSubject().getPrincipal();
        return ControllerUtil.getSuccessResultBySelf(sxCorporationService.findByStuNo(sxStudent.getStuNo()));
    }


    @ApiOperation(value = "学生设置/修改职位", httpMethod = "POST")
    @RequiresRoles(RoleCode.STUDENT)
    @PostMapping("/student/position")
    public ResponseVO setPosition(String position) {
        SxStudent sxStudent = (SxStudent) SecurityUtils.getSubject().getPrincipal();
        sxStudentService.updatePosition(sxStudent.getStuNo(), position);
        return ControllerUtil.getSuccessResultBySelf(sxStudentService.findByStuNo(sxStudent.getStuNo()));
    }

    @ApiOperation(value = "学生修改密码", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "oldPassword", value = "旧密码", required = true),
            @ApiImplicitParam(name = "newPassword", value = "新密码", required = true)
    })
    @RequiresRoles(RoleCode.STUDENT)
    @PostMapping("/student/password")
    public ResponseVO setPassword(String oldPassword, String newPassword) {
        String msg = null;
        if ((msg = StrongPwdValidator.validate(newPassword).get(false))!= null){
            return ControllerUtil.getFalseResultMsgBySelf(msg);
        }
        SxStudent sxStudent = (SxStudent) SecurityUtils.getSubject().getPrincipal();

        boolean flag = sxStudentService.updatePassword(sxStudent.getStuNo(), oldPassword, newPassword);
        if (flag) {
            return ControllerUtil.getSuccessResultBySelf("修改成功 success");
        } else {
            return ControllerUtil.getFalseResultMsgBySelf("原密码错误old password is wrong");

        }

    }

    @ApiOperation(value = "通过学号下载报告册PDF", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "stdNo", value = "学生学号", required = true),
            @ApiImplicitParam(name = "password", value = "密码123456", required = true),
    })
    @PostMapping("/report/pdf")
    public ResponseVO getReportPdf(@RequestParam String stdNo, @RequestParam String password) throws OfficeException {
        if (!password.equals(psd)) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_VALIDATION_ERROR);
        }
        SxStudent sxStudent = sxStudentService.findSelfInfoByStuNo(stdNo);
        if(sxStudent==null){return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_VALIDATION_ERROR);}
        SxReport sxReport = sxReportService.getReportInfo(sxStudent.getStuNo());
        SxTeacher sxTeacher = sxTeacherService.findByTeacherNo(sxStudent.getTeacherNo());
        EntityUtil.setNullFiledToString(sxStudent);
        EntityUtil.setNullFiledToString(sxReport);
        EntityUtil.setNullFiledToString(sxTeacher);
        Map<String, String> params = new HashMap<>();
        params.put("${stu_name}", sxStudent.getName());
        params.put("${stu_no}", sxStudent.getStuNo());
        params.put("${college}", sxStudent.getCollege());
        params.put("${major}", sxStudent.getMajor());
        params.put("${corp_name}", sxStudent.getCorpName());
        params.put("${corp_position}", sxStudent.getCorpPosition());
        params.put("${stage1_guide_date}", sxReport.getStage1GuideDate());
        params.put("${stage1_guide_way}", sxReport.getStage1GuideWay());
        params.put("${stage1_summary}", sxReport.getStage1Summary());
        params.put("${stage1_comment}", sxReport.getStage1Comment());
        params.put("${stage1_grade}", sxReport.getStage1Grade());
        params.put("${stage2_guide_date}", sxReport.getStage2GuideDate());
        params.put("${stage2_guide_way}", sxReport.getStage2GuideWay());
        params.put("${stage2_summary}", sxReport.getStage2Summary());
        params.put("${stage2_comment}", sxReport.getStage2Comment());
        params.put("${stage2_grade}", sxReport.getStage2Grade());
        params.put("${teacher}", sxTeacher.getName());
        params.put("${total_grade}", sxReport.getTotalGrade());
        params.put("${total_score}", sxReport.getTotalScore());
        params.put("${gmt_start}", DateUtil.getUpperDate(sxReport.getGmtStart()));
        params.put("${gmt_end}", DateUtil.getUpperDate(sxReport.getGmtEnd()));
        params.put("${fill_date}", DateUtil.getNowUpperDate());
        String wordFileName = RenderWordUtil.exportWordToResponse("report", sxStudent.getStuNo(), params);
        if (wordFileName != null) {
            String path = System.getProperty("user.dir") + (SystemUtil.isWindows()?"\\static\\":"/static/");
            String pdfFileName = wordFileName.replace("docx", "pdf");
            // 源文件 （office）
            File source = new File(path + wordFileName);
            // 目标文件 （pdf）
            File target = new File(path + pdfFileName);
            // 转换文件
            if (!target.exists()) {
                documentConverter.convert(source).to(target).execute();
                //删除word
                source.delete();
            }
            return ControllerUtil.getSuccessResultBySelf(pdfFileName);
        }
        return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_SERVER_ERROR);
    }


    @ApiOperation(value = "通过学号下载鉴定表PDF", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "stdNo", value = "学生学号", required = true),
            @ApiImplicitParam(name = "password", value = "密码123456", required = true),
    })
    @PostMapping("/identify/pdf")
    @RequiresRoles(RoleCode.STUDENT)
    public ResponseVO getIdentifyPdf(@RequestParam String stdNo, @RequestParam String password) throws OfficeException {
        if (!password.equals(psd)) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_VALIDATION_ERROR);
        }
        SxStudent sxStudent = sxStudentService.findSelfInfoByStuNo(stdNo);
        if(sxStudent==null){return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_VALIDATION_ERROR);}
        SxIdentifyForm sxIdentifyForm = sxIdentifyFormService.getIdentifyInfo(sxStudent.getStuNo());
        EntityUtil.setNullFiledToString(sxStudent);
        EntityUtil.setNullFiledToString(sxIdentifyForm);
        Map<String, String> params = new HashMap<>();
        params.put("${college}", sxStudent.getCollege());
        params.put("${major}", sxStudent.getMajor());
        params.put("${stu_name}", sxStudent.getName());
        params.put("${stu_no}", sxStudent.getStuNo());
        params.put("${corp_name}", sxStudent.getCorpName());
        params.put("${gmt_start}", DateUtil.getUpperDate(sxIdentifyForm.getGmtStart()));
        params.put("${gmt_end}", DateUtil.getUpperDate(sxIdentifyForm.getGmtEnd()));
        params.put("${fill_date}", DateUtil.getNowUpperDate());
        params.put("${content}", sxIdentifyForm.getSxContent());
        params.put("${self_summary}", sxIdentifyForm.getSelfSummary());
        params.put("${corp_teacher_opinion}", sxIdentifyForm.getCorpTeacherOpinion());
        params.put("${corp_opinion}", sxIdentifyForm.getCorpOpinion());
        params.put("${teacher_grade}", sxIdentifyForm.getTeacherGrade());
        params.put("${comprehsv_grade}", sxIdentifyForm.getComprehsvGrade());
        params.put("${college_principal_opinion}", sxIdentifyForm.getCollegePrincipalOpinion());
        params.put("${corp_teacher_score}", sxIdentifyForm.getCorpTeacherScore());
        String wordFileName = RenderWordUtil.exportWordToResponse("identify", sxStudent.getStuNo(), params);
        if (wordFileName != null) {
            String path = System.getProperty("user.dir") + (SystemUtil.isWindows()?"\\static\\":"/static/");
            String pdfFileName = wordFileName.replace("docx", "pdf");
            // 源文件 （office）
            File source = new File(path + wordFileName);
            // 目标文件 （pdf）
            File target = new File(path + pdfFileName);
            // 转换文件
            if (!target.exists()) {
                documentConverter.convert(source).to(target).execute();
                //删除word
                source.delete();
            }
            return ControllerUtil.getSuccessResultBySelf(pdfFileName);
        }
        return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_SERVER_ERROR);
    }

    @ApiOperation(value = "通过学号列表下载鉴定表PDF", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "password", value = "密码123456", required = true),
    })
    @PostMapping("/identifys/pdf")
    @RequiresRoles(RoleCode.STUDENT)
    public ResponseVO getidentifysPdf(@RequestParam List<String> stuNoLists,@RequestParam String password) throws OfficeException {
        if (!password.equals(psd)) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_VALIDATION_ERROR);
        }
        for (String stuNoList:stuNoLists){
            logger.info("--------当前打印学生--------------");
            logger.info(stuNoList);
            SxStudent sxStudent = sxStudentService.findSelfInfoByStuNo(stuNoList);
            SxIdentifyForm sxIdentifyForm = sxIdentifyFormService.getIdentifyInfo(sxStudent.getStuNo());
            EntityUtil.setNullFiledToString(sxStudent);
            EntityUtil.setNullFiledToString(sxIdentifyForm);
            Map<String, String> params = new HashMap<>();
            params.put("${college}", sxStudent.getCollege());
            params.put("${major}", sxStudent.getMajor());
            params.put("${stu_name}", sxStudent.getName());
            params.put("${stu_no}", sxStudent.getStuNo());
            params.put("${corp_name}", sxStudent.getCorpName());
            params.put("${gmt_start}", DateUtil.getUpperDate(sxIdentifyForm.getGmtStart()));
            params.put("${gmt_end}", DateUtil.getUpperDate(sxIdentifyForm.getGmtEnd()));
            params.put("${fill_date}", DateUtil.getNowUpperDate());
            params.put("${content}", sxIdentifyForm.getSxContent());
            params.put("${self_summary}", sxIdentifyForm.getSelfSummary());
            params.put("${corp_teacher_opinion}", sxIdentifyForm.getCorpTeacherOpinion());
            params.put("${corp_opinion}", sxIdentifyForm.getCorpOpinion());
            params.put("${teacher_grade}", sxIdentifyForm.getTeacherGrade());
            params.put("${comprehsv_grade}", sxIdentifyForm.getComprehsvGrade());
            params.put("${college_principal_opinion}", sxIdentifyForm.getCollegePrincipalOpinion());
            params.put("${corp_teacher_score}", sxIdentifyForm.getCorpTeacherScore());
            String wordFileName = RenderWordUtil.exportWordToResponse("identify", sxStudent.getStuNo(), params);
            if (wordFileName != null) {
                String path = System.getProperty("user.dir") + (SystemUtil.isWindows()?"\\static\\":"/static/");
                String pdfFileName = wordFileName.replace("docx", "pdf");
                // 源文件 （office）
                File source = new File(path + wordFileName);
                // 目标文件 （pdf）
                File target = new File(path + pdfFileName);
                // 转换文件
                if (!target.exists()) {
                    documentConverter.convert(source).to(target).execute();
                    //删除word
                    source.delete();
                }
            }
        }
        return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_SUCCESS);
    }

    @ApiOperation(value = "通过学号列表下载报告册PDF", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "password", value = "密码123456", required = true),
    })
    @PostMapping("/reports/pdf")
    @RequiresRoles(RoleCode.STUDENT)
    public ResponseVO getReportsPdf(@RequestParam List<String> stuNoLists,@RequestParam String password) throws OfficeException {
        if (!password.equals(psd)) {
            return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_VALIDATION_ERROR);
        }
        for (String stuNoList:stuNoLists){
            logger.info("--------当前打印学生--------------");
            logger.info(stuNoList);
            SxStudent sxStudent = sxStudentService.findSelfInfoByStuNo(stuNoList);
            SxReport sxReport = sxReportService.getReportInfo(sxStudent.getStuNo());
            SxTeacher sxTeacher = sxTeacherService.findByTeacherNo(sxStudent.getTeacherNo());
            EntityUtil.setNullFiledToString(sxStudent);
            EntityUtil.setNullFiledToString(sxReport);
            EntityUtil.setNullFiledToString(sxTeacher);
            Map<String, String> params = new HashMap<>();
            params.put("${stu_name}", sxStudent.getName());
            params.put("${stu_no}", sxStudent.getStuNo());
            params.put("${college}", sxStudent.getCollege());
            params.put("${major}", sxStudent.getMajor());
            params.put("${corp_name}", sxStudent.getCorpName());
            params.put("${corp_position}", sxStudent.getCorpPosition());
            params.put("${stage1_guide_date}", sxReport.getStage1GuideDate());
            params.put("${stage1_guide_way}", sxReport.getStage1GuideWay());
            params.put("${stage1_summary}", sxReport.getStage1Summary());
            params.put("${stage1_comment}", sxReport.getStage1Comment());
            params.put("${stage1_grade}", sxReport.getStage1Grade());
            params.put("${stage2_guide_date}", sxReport.getStage2GuideDate());
            params.put("${stage2_guide_way}", sxReport.getStage2GuideWay());
            params.put("${stage2_summary}", sxReport.getStage2Summary());
            params.put("${stage2_comment}", sxReport.getStage2Comment());
            params.put("${stage2_grade}", sxReport.getStage2Grade());
            params.put("${teacher}", sxTeacher.getName());
            params.put("${total_grade}", sxReport.getTotalGrade());
            params.put("${total_score}", sxReport.getTotalScore());
            params.put("${gmt_start}", DateUtil.getUpperDate(sxReport.getGmtStart()));
            params.put("${gmt_end}", DateUtil.getUpperDate(sxReport.getGmtEnd()));
            params.put("${fill_date}", DateUtil.getNowUpperDate());
            String wordFileName = RenderWordUtil.exportWordToResponse("report", sxStudent.getStuNo(), params);
            if (wordFileName != null) {
                String path = System.getProperty("user.dir") + (SystemUtil.isWindows()?"\\static\\":"/static/");
                String pdfFileName = wordFileName.replace("docx", "pdf");
                // 源文件 （office）
                File source = new File(path + wordFileName);
                // 目标文件 （pdf）
                File target = new File(path + pdfFileName);
                // 转换文件
                if (!target.exists()) {
                    documentConverter.convert(source).to(target).execute();
                    //删除word
                    source.delete();
                }
            }
        }
        return ControllerUtil.getFalseResultMsgBySelf(RespCode.MSG_SUCCESS);
    }

}
