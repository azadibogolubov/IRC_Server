 /*
  * Author: Azadi Bogolubov
  */

package com.tutorazadi.cs594project;

import java.util.ArrayList;
import java.util.List;

//Channels have names, a list of members, and a list of operators.
public class Channel
{
	private String name;
	private List<String> channelMembers = new ArrayList<String>();
	private List<String> messages = new ArrayList<String>();
	private boolean isPrivate;
	private int numMembers = 0;
	
	// Constructors
	/* 
	 * Constructor #1:
	 * Called when we are creating the common room at load time.
	 */
	public Channel(String name, boolean privacy)
	{
		this.name = name;
		isPrivate = privacy;
		messages.add("Welcome to " + name);
	}
	
	/* 
	 * Constructor #2:
	 * Called when we are creating a room with members.
	 */
	public Channel(String name, boolean privacy, String... nick)
	{
		this.name = name;
		isPrivate = privacy;
		messages.add("Welcome to " + name);
		for (String n: nick)
			channelMembers.add(n);
	}
	
	// Message accessor/mutator methods
	public List<String> getMessages()
	{
		return messages;
	}

	public void addMessage(String s)
	{
		messages.add(s);
	}
	
	// Channel member accessor/mutator methods
	public List<String> getChannelMembers()
	{
		return channelMembers;
	}

	public void addChannelMember(String m)
	{
		channelMembers.add(m);
	}
	
	public void removeChannelMember(String m)
	{
		channelMembers.remove(m);
	}
	
	// Name accessor/mutator methods
	public void setName(String value)
	{
		name = value;
	}
	
	public String getName()
	{
		return name;
	}
	
	// Room privacy accessor/mutator methods
	public boolean isPrivate()
	{
		return isPrivate;
	}
	
	public void setPrivacy(boolean privacySetting)
	{
		isPrivate = privacySetting;
	}
	
	public int getNumMembers()
	{
		return numMembers;
	}
	
	public void setNumMembers(int value)
	{
		numMembers = value;
	}
}