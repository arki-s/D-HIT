package analix.DHIT.controller;


import analix.DHIT.input.ReportCreateInput;
import analix.DHIT.input.ReportSearchInput;
import analix.DHIT.input.ReportUpdateInput;
import analix.DHIT.model.Report;
import analix.DHIT.model.TaskLog;
import analix.DHIT.model.User;
import analix.DHIT.service.ReportService;
import analix.DHIT.service.TaskLogService;
import analix.DHIT.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final UserService userService;
    private final TaskLogService taskLogService;
    private final ReportService reportService;

    public MemberController(UserService userService, TaskLogService taskLogService, ReportService reportService) {
        this.userService = userService;
        this.taskLogService = taskLogService;
        this.reportService = reportService;
    }

    @GetMapping("/report/create")
    public String displayReportCreate(
            Model model
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //バリデーションInteger
        int employeeCode = Integer.parseInt(authentication.getName());
        //employeeCodeを使用し、直近のreportがあるか調べる(取得)
        String latestReportId = reportService.getLatestIdByEmployeeCode(employeeCode);
        ReportCreateInput reportCreateInput = new ReportCreateInput();
        //java.timeパッケージから現在の時刻を取得
        reportCreateInput.setDate(LocalDate.now());

        if (latestReportId == null) {
            model.addAttribute("reportCreateInput", reportCreateInput);
            return "member/report-create";
        }
        //以下reportがひとつでもあった場合の処理
        //既存のreportidを参照にreportModelの値をすべてset
        Report report = reportService.getReportById(Integer.parseInt(latestReportId));
        //(前日のreport内容を引継ぎ入力欄に記入)
        reportCreateInput.setStartTime(report.getStartTime());
        reportCreateInput.setEndTime(report.getEndTime());
        //report_idを参照してtask_Logの値を取得しset
        reportCreateInput.setTaskLogs(taskLogService.getIncompleteTaskLogsByReportId(Integer.parseInt(latestReportId)));

        model.addAttribute("reportCreateInput", reportCreateInput);
        return "member/report-create";

    }

    //↓Transactionalはトランザクション処理で一連の流れが失敗した場合ロールバックする
    @Transactional
    @PostMapping("/report/create")
    public String createReport(ReportCreateInput reportCreateInput, RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int employeeCode = Integer.parseInt(authentication.getName());


        if (reportService.existsReport(employeeCode, reportCreateInput.getDate())) {
            redirectAttributes.addFlashAttribute("error", reportCreateInput.getDate() + "は既に業務報告書が存在しています");
            return "redirect:/member/report/create";
        }

        //newReportIdには新たにInsertされたreportのIDが入る
        int newReportId = reportService.create(
                employeeCode,
                reportCreateInput.getCondition(),
                reportCreateInput.getImpressions(),
                reportCreateInput.getTomorrowSchedule(),
                reportCreateInput.getDate(),
                reportCreateInput.getEndTime(),
                reportCreateInput.getStartTime(),
                reportCreateInput.getIsLateness(),
                reportCreateInput.getLatenessReason(),
                reportCreateInput.getIsLeftEarly()
        );

        // タスクが存在するならタスクログに追加
//        if (reportCreateInput.getTaskLogs() != null) {
//            List<TaskLog> taskLogs = reportCreateInput.getTaskLogs();
//            taskLogs.forEach(x -> x.setReportId(newReportId));
//            for (TaskLog taskLog : taskLogs) {
//                if (taskLog != null && taskLog.getName() != null) {
//                    taskLogService.create(taskLog);
//                }
//            }
//        }

        return "redirect:/member/report/create-completed";
    }

    @GetMapping("/report/create-completed")
    public String displayReportCreateCompleted(
    ) {
        return "member/report-create-completed";
    }

    @GetMapping("/report-search")
    public String displayReportSearch(
            Model model
    ) {
        model.addAttribute("reportSearchInput", new ReportSearchInput());
        model.addAttribute("error", model.getAttribute("error"));

        return "member/report-search";
    }

    @PostMapping("/search-report")
    public String searchReport(
            ReportSearchInput reportSearchInput,
            RedirectAttributes redirectAttributes
    ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int employeeCode = Integer.parseInt(authentication.getName());

        String reportId = reportService.searchId(
                employeeCode,
                reportSearchInput.getDate()
        );

        if (reportId == null) {
            redirectAttributes.addFlashAttribute("error", "ヒットしませんでした");
            return "redirect:/member/report-search";
        }

        redirectAttributes.addAttribute("reportId", reportId);
        return "redirect:/member/reports/{reportId}";
    }

    @GetMapping("/reports/{reportId}")
    public String displayReportDetail(@PathVariable("reportId") int reportId, Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int employeeCode = Integer.parseInt(authentication.getName());

        Report report = reportService.getReportById(reportId);
        if (report.getEmployeeCode() != employeeCode) {
            return "redirect:/member/report/create";
        }

        List<TaskLog> taskLogs = taskLogService.getTaskLogsByReportId(reportId);
        User member = userService.getUserByEmployeeCode(report.getEmployeeCode());

        model.addAttribute("report", report);
        model.addAttribute("taskLogs", taskLogs);
        model.addAttribute("member", member);

        model.addAttribute("beforeReportId", reportService.getBeforeIdById(reportId));
        model.addAttribute("afterReportId", reportService.getAfterIdById(reportId));

        String date = report.getDate().format(DateTimeFormatter.ofPattern("yyyy年M月d日(E)", Locale.JAPANESE));
        model.addAttribute("date", date);

        return "member/report-detail";
    }

    @PostMapping("/reports/{reportId}/delete")
    @Transactional
    public String deleteReport(
            @PathVariable int reportId
    ) {
        Report report = reportService.getReportById(reportId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int employeeCode = Integer.parseInt(authentication.getName());

        if (report.getEmployeeCode() != employeeCode) {
            return "redirect:/member/report/create";
        }

        this.taskLogService.deleteByReportId(reportId);
        this.reportService.deleteById(reportId);

        return "redirect:/member/report/delete-completed";
    }

    @GetMapping("/report/delete-completed")
    public String displayReportDeleteCompleted(
    ) {
        return "member/report-delete-completed";
    }

    @GetMapping("/reports/{reportId}/edit")
    public String displayReportEdit(
            Model model,
            @PathVariable int reportId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int employeeCode = Integer.parseInt(authentication.getName());

        Report report = this.reportService.getReportById(reportId);

        if (report.getEmployeeCode() != employeeCode) {
            return "redirect:/member/report/create";
        }

        List<TaskLog> taskLogs = this.taskLogService.getTaskLogsByReportId(reportId);

        model.addAttribute("report", report);
        model.addAttribute("taskLogs", taskLogs);
        model.addAttribute("reportUpdateInput", new ReportUpdateInput());

        return "member/report-edit";

    }

    @Transactional
    @PostMapping("/report/update")
    public String updateReport(ReportUpdateInput reportUpdateInput, RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int employeeCode = Integer.parseInt(authentication.getName());

        Report report = this.reportService.getReportById(reportUpdateInput.getReportId());

        if (report.getEmployeeCode() != employeeCode) {
            return "redirect:/member/report/create";
        }

        this.reportService.update(reportUpdateInput);
        this.taskLogService.deleteByReportId(reportUpdateInput.getReportId());

        if (reportUpdateInput.getTaskLogs() != null) {
            List<TaskLog> taskLogs = reportUpdateInput.getTaskLogs();
            taskLogs.forEach(x -> x.setReportId(reportUpdateInput.getReportId()));
            for (TaskLog taskLog : taskLogs) {
                if (taskLog != null && taskLog.getName() != null) {
                    taskLogService.create(taskLog);
                }
            }
        }

        redirectAttributes.addAttribute("reportId", reportUpdateInput.getReportId());
        return "redirect:/member/reports/{reportId}";

    }


}
