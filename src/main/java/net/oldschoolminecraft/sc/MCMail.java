package net.oldschoolminecraft.sc;

public class MCMail
{
    public String sender;
    public String recipient;
    public String message;
    public long timestamp;

    public MCMail() {}

    public MCMail(String sender, String recipient, String message)
    {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isValidMessage()
    {
        return message.length() <= 100 && !(recipient == null || recipient.isEmpty() && !(sender == null || sender.isEmpty())); // all conditions met
    }
}
