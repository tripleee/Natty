package in.bhargavrao.stackoverflow.natty.services;

import fr.tunaki.stackoverflow.chat.Message;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import in.bhargavrao.stackoverflow.natty.commands.Check;
import in.bhargavrao.stackoverflow.natty.exceptions.FeedbackInvalidatedException;
import in.bhargavrao.stackoverflow.natty.utils.CheckUtils;
import in.bhargavrao.stackoverflow.natty.utils.CommandUtils;

import java.io.IOException;

public class ReplyHandlerService {

    public void reply(Room room, PingMessageEvent event, String sitename, String siteurl, boolean isReply){
        Message message = event.getMessage();
        System.out.println(message.getContent());
        if (CheckUtils.checkIfUserIsBlacklisted(event.getUserId())){
            System.out.println("Blacklisted user");
            return;
        }
        if (CommandUtils.checkForCommand(message.getContent(),"tp") ||
                CommandUtils.checkForCommand(message.getContent(),"t")){
            store(room, event, "tp", sitename, siteurl);
        }
        if (CommandUtils.checkForCommand(message.getContent(),"ne") ||
                CommandUtils.checkForCommand(message.getContent(),"n")){
            store(room, event, "ne", sitename, siteurl);
        }
        if (CommandUtils.checkForCommand(message.getContent(),"fp") ||
                CommandUtils.checkForCommand(message.getContent(),"f")){
            store(room, event, "fp", sitename, siteurl);
        }
        if (CommandUtils.checkForCommand(message.getContent(),"delete") ||
                CommandUtils.checkForCommand(message.getContent(),"remove")){
            long repliedTo = event.getParentMessageId();
            room.delete(repliedTo);
        }
        if (CommandUtils.checkForCommand(message.getContent(),"why")){
            long repliedTo = event.getParentMessageId();
            Message repliedToMessage = room.getMessage(repliedTo);
            String linkToPost = getPostIdFromMessage(repliedToMessage.getPlainContent().trim(), siteurl);

            try {
                String returnParams[] = new Check(message, sitename, siteurl).getCheckData(linkToPost, 2);
                room.replyTo(message.getId(), returnParams[0]);
                if (!returnParams[1].equals(""))
                    room.send(returnParams[1]);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void store(Room room, PingMessageEvent event, String type, String sitename, String siteurl){
        long repliedTo = event.getParentMessageId();
        Message repliedToMessage = room.getMessage(repliedTo);
        String message = repliedToMessage.getPlainContent().trim();
        String linkToPost = getPostIdFromMessage(message, siteurl);
        StorageService service = new FileStorageService();
        if (!type.equals("tp") && service.checkAutoFlag(Long.parseLong(linkToPost),sitename)){
            room.send("False positive feedback on Autoflag, please retract @Bhargav or @Petter");
        }
        try {
            new FeedbackHandlerService(sitename, siteurl).handleFeedback(event.getMessage().getUser(), type, linkToPost);
        } catch (FeedbackInvalidatedException e) {
            e.printStackTrace();
            room.send(e.getMessage());
        }
    }

    private String getPostIdFromMessage(String message, String siteurl) {
        message = message.split("//"+siteurl+"/a/")[1];
        return message.substring(0,message.indexOf(")"));
    }

}
