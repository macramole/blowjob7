package art.leandrogarber.blowjob;

public class CommunicationMessage {
	public String message;
	public CommunicationThread sender;
	
	public CommunicationMessage(String message, CommunicationThread sender)
	{
		this.message = message;
		this.sender = sender;
	}
}
