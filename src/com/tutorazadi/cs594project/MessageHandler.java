 /*
  * Author: Azadi Bogolubov
  */

package com.tutorazadi.cs594project;

public class MessageHandler 
{
    public static String handleSystemMessage(String in)
    {
    	// Syntax: QUIT
    	// Result: broadcastCode;QUIT;;
    	if (in.contains("QUIT"))
    		return "0;QUIT;;";
    	
    	// Syntax: NICK <nickName>
    	// Result: broadcastCode;NICK;;<nickName>
    	else if (in.contains("NICK"))
    		return "0;NICK;;" + in.substring(6);
    	
    	// Syntax: LIST
    	// Result: broadcastCode;LIST;;
    	else if (in.contains("LIST"))
    		return "0;LIST;;";
    	
    	else
    		return "0;;;";
    }
    
    public static String handleChannelMessage(String in)
    {
    	// Syntax: SWITCH <channelName>
    	// Result: broadcastCode;SWITCH;<channelName>;
    	if (in.contains("SWITCH"))
    		return "0;SWITCH;" + in.substring(7) +";";
    	
    	// Syntax: MEMBERS <channelName>
    	// Result: broadcastCode;MEMBERS;<channelName>;
    	else if (in.contains("MEMBERS"))
    		return "0;MEMBERS;" + in.substring(9) +";";
    	
    	// Syntax: JOIN <channelName>
    	// Result: broadcastCode;JOIN;<channelName>;
    	else if (in.contains("JOIN"))
    		return "0;JOIN;" + in.substring(6) + ";";
    	
    	// Syntax: PART <channelName>
    	// Result: broadcastCode;PART;<channelName>;
    	else if (in.contains("PART"))
    		return "0;PART;" + in.substring(6) + ";";    	
    	else
    		return "0;;;";
    }
    
    // Handles one or more channels...
    public static String handleMultiChannelMessage(String in)
    {
    	// Syntax: MULT <channelName> [, channelName]
    	// Result: broadcastCode;MULT;[<channelName>];message
    	String segmentedMsg = in.substring(5);
    	String[] messages = segmentedMsg.split("]");
    	messages[0] = messages[0].substring(2);
    	messages[1] = messages[1].substring(1);
    	return "1;MULT;" + messages[0] + ";" + messages[1];
    }
    
    public static String handleSingleChannelMessage(String in)
    {
    	// Syntax: <message>
    	// Result: broadcastCode;SINGLE;<channelName>;message
    	String[] segmentedMsg = in.split(";");
    	return "1;SINGLE;" + segmentedMsg[0] + ";" + segmentedMsg[1];    	
    }
    
    public static String handleP2PMessage(String in)
    {
    	// Syntax: MSG <nick>
    	// Result: broadcastCode;MSG;<nick>;
    	return "2;MSG;" + in.substring(5) + ";";
    }
    
    public static String kick()
    {
    	return "1;BADWORD;;";
    }    
}
