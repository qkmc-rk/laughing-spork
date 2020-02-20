package xyz.ruankun.laughingspork.service;

import xyz.ruankun.laughingspork.entity.*;


import java.sql.Date;
import java.util.List;

public interface SxStudentService {

/**
 * 学生服务类接口
 *
 *
 */

    /**
     * 学生查看自己信息(SxStudent)
     *
     * @param StuNo :
     * @return: xyz.ruankun.laughingspork.entity.SxStudent
     */
    SxStudent findSelfInfoByStuNo(String StuNo);

    /**
     * 返回这个学生相应的校内导师信息
     *
     * @param sxStudent :
     * @return: xyz.ruankun.laughingspork.entity.SxTeacher
     */

    SxTeacher getTeacherInfo(SxStudent sxStudent);

    /**
     * 返回这个学生的鉴定表信息
     *
     * @param sxStudent :
     * @return: xyz.ruankun.laughingspork.entity.SxIdentifyForm
     */

    SxIdentifyForm getSelfIdentifyInfo(SxStudent sxStudent);

    /**
     * 返回这个学生的报告册信息
     *
     * @param sxStudent :
     * @return: xyz.ruankun.laughingspork.entity.SxReport
     */

    SxReport getSelfReportInfo(SxStudent sxStudent);


    /**
     * 学生保存鉴定表内容
     *
     * @param practiceContent :
     * @param selfSummary     :
     * @return: xyz.ruankun.laughingspork.entity.SxStudent
     */
    SxIdentifyForm saveIdentifyForm(SxStudent sxStudent, String practiceContent, String selfSummary, String corpOpinion, String corpTeacherOpinion);

    /**
     * 保存实习阶段的实习总结
     *
     * @param sxStudent     :
     * @param stage1Summary :
     * @return: xyz.ruankun.laughingspork.entity.SxReport
     */
    SxReport setStage1Summary(Date stage1Date,Date gmtStart ,SxStudent sxStudent, String stage1Summary, String stage1GuideWay, String stage1GuideDate);

    SxReport setStage2Summary(Date stage2Date,Date gmtEnd,SxStudent sxStudent, String stage2Summary, String stage2GuideWay, String stage2GuideDate);

    void save(SxStudent sxStudent);

    SxStudent findByStuNo(String StuNo);

    Boolean testAuth(String tNo, String stuNO);

    SxStagemanage getNowReportStage();

    List<SxTeacher> collegeTeacher(SxStudent sxStudent);

    SxStudent choseCollegeTeacher(SxStudent sxStudent, String tNO);

    void updatePosition(String stuNo, String position);

    boolean updatePassword(String inputOldPassword, String oldPassword, String newPassword);
}

