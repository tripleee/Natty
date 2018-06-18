package in.bhargavrao.stackoverflow.natty.services;

import fr.tunaki.stackoverflow.chat.User;
import in.bhargavrao.stackoverflow.natty.exceptions.FeedbackInvalidatedException;
import in.bhargavrao.stackoverflow.natty.model.Feedback;
import in.bhargavrao.stackoverflow.natty.model.FeedbackType;
import in.bhargavrao.stackoverflow.natty.model.Reason;
import in.bhargavrao.stackoverflow.natty.model.SavedReport;
import in.bhargavrao.stackoverflow.natty.utils.FeedbackUtils;
import in.bhargavrao.stackoverflow.natty.utils.PostUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FeedbackHandlerService {

    private String sitename;
    private String siteurl;

    public FeedbackHandlerService(String sitename, String siteurl) {
        this.sitename = sitename;
        this.siteurl = siteurl;
    }

    public void handleFeedback(User user, String type, String answerId) throws FeedbackInvalidatedException {
        StorageService service = new FileStorageService();
        String sentinel = service.getSentinelId(answerId, sitename);
        long postId = -1;
        if (sentinel!=null) {
            postId = Long.parseLong(sentinel.split(",")[1]);
        }
        if(postId!=-1) {
            long feedbackId = PostUtils.addFeedback(postId, user.getId(), user.getName(), type, sitename, siteurl);
        }
        FeedbackType previousFeedbackType = service.getFeedback(answerId, sitename);
        Feedback feedback = new Feedback(user.getName(), user.getId(), FeedbackUtils.getFeedbackTypeFromFeedback(type));

        if(previousFeedbackType==null) {
            String loggedLine = service.retrieveReport(answerId, sitename);
            SavedReport report = getSavedReportFromLog(loggedLine);
            service.saveFeedback(feedback, report, sitename);
        }
        else if (previousFeedbackType.equals(feedback.getFeedbackType())){
            String loggedLine = service.retrieveFeedback(answerId, sitename);
            SavedReport report = getSavedReportFromLog(loggedLine.replace(previousFeedbackType.toString()+",",""));
            service.addFeedback(feedback, report, sitename);
        }
        else{
            String loggedLine = service.retrieveFeedback(answerId, sitename);
            SavedReport report = getSavedReportFromLog(loggedLine.replace(previousFeedbackType.toString()+",",""));
            service.invalidateFeedback(feedback, report, sitename);
            throw new FeedbackInvalidatedException("https://"+siteurl+"/a/"+report.getAnswerId());
        }
    }

    private SavedReport getSavedReportFromLog(String logline){
        SavedReport report = new SavedReport();
        String parts[] = logline.split(",");
        if (parts.length!=6)
            return null;
        report.setAnswerId(Integer.valueOf(parts[0]));
        report.setTimestamp(Instant.parse(parts[1]));
        report.setNaaValue(Double.parseDouble(parts[2]));
        report.setBodyLength(Integer.parseInt(parts[3]));
        report.setReputation(Integer.parseInt(parts[4]));
        List<Reason> reasons = getReasons(parts[5]);
        report.setReasons(reasons);
        return report;
    }

    private List<Reason> getReasons(String part) {
        List<Reason> reasons = new ArrayList<>();
        String reasonStrings[] = part.split(";");
        for (String reasonString: reasonStrings){
            Reason reason = new Reason();
            if (reasonString.contains("-")){
                reason.setReasonName(reasonString.split(" - ")[0]);
                reason.setSubReason(reasonString.split(" - ")[1]);
            }
            else {
                reason.setReasonName(reasonString);
            }
            reasons.add(reason);
        }
        return reasons;
    }

}
