package message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import worldmodel.jobs.Task;
import worldmodel.jobs.Token;
/**
 *
 * @author rick
 */
public class MyMessage implements Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private MessageType type;
	private Task task;
	private Integer sender;
	private List<Token> token = new ArrayList<Token>();
	
	public MyMessage(MessageType type, Task task){
		this.setType(type);
		this.setTask(task);
	}
	
	public MyMessage(MessageType type, List<Token> token)
	{
		this.setType(type);
		this.setToken(token);
	}
	
	public MyMessage(MessageType type, Token token)
	{
		this.setType(type);
		this.addToken(token);
	}
	
	public MessageType getType() {
		return type;
	}
	
	public void setType(MessageType type) {
		this.type = type;
	}
	
	public Task getTask() {
		return task;
	}
	
	public void setTask(Task task) {
		this.task = task;
	}

	public List<Token> getToken() {
		return token;
	}

	public void setToken(List<Token> token) {
		this.token = token;
	}
	
	public void addToken(Token token)
	{
		this.token.add(token);
	}

	public Integer getSender() {
		return sender;
	}

	public void setSender(Integer sender) {
		this.sender = sender;
	}
	
	
	

}
