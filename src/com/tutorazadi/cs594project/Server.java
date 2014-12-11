 /*
  * Author: Azadi Bogolubov
  */

package com.tutorazadi.cs594project;

// Sources: 
// Java example from class slides

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class Server 
{
	// Variable declarations.
    private final int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private ByteBuffer buf = ByteBuffer.allocate(256);
    private HashMap<String, String> nickPairs = new HashMap<String, String>();
    private List<String> nicks = new ArrayList<String>();
    private List<Channel> channels = new ArrayList<Channel>();
    private int channelIndex, broadcastCode;
    private boolean foundChannel;
    private List<String> keys = new ArrayList<String>();
    
    // Default constructor.
    public Server(int port) throws IOException 
    {
        this.port = port;
        this.ssc = ServerSocketChannel.open();
        this.ssc.socket().bind(new InetSocketAddress(port));
        this.ssc.configureBlocking(false);
        this.selector = Selector.open();
        this.ssc.register(selector, SelectionKey.OP_ACCEPT);
        
        try 
        {
            System.out.println("Server starting on port " + this.port);
 
            Iterator<SelectionKey> iter;
            SelectionKey key;
            while(this.ssc.isOpen()) 
            {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while(iter.hasNext()) 
                {
                    key = iter.next();
                    iter.remove();
                    if(key.isAcceptable()) 
                        this.handleAccept(key);
                    if(key.isReadable()) 
                        this.handleRead(key);
                }
            }
            channels.add(new Channel("COMMON", true));
        }
        catch(IOException e) { System.out.println("IOException, server of port " + this.port + " terminating."); }
    }
 
    private void sendFile(String fileName, SelectionKey key)
    {
    	try
    	{
	    	File myFile = new File(fileName.replace(".txt", "") + "result.txt");
	    	byte [] mybytearray  = new byte [(int)myFile.length()];
	    	ByteBuffer bb = ByteBuffer.wrap(mybytearray);
	    	SocketChannel sc = (SocketChannel) key.channel();
	    	sc.write(bb);
    	}
    	catch (IOException e)
    	{
    		System.out.println("File transfer error:");
    		e.printStackTrace();
    	}
    }
    
    // Gets the nickname of the current user...
    private String getNick(SelectionKey key)
    {
    	return nickPairs.get(key.attachment().toString());
    }

    // Iterate over all the channels and search for a match
    // Running time: O(n)
    private void checkChannelsForMatch(String channelName)
    {
    	foundChannel = false;
    	for (Channel c : channels)
    		// If we have found the channel we are looking for, break out of the loop...
    		// Otherwise, continue.
    		if (c.getName().equals(channelName))
        	{
    			foundChannel = true;
    			channelIndex = channels.indexOf(c);
    			break;
        	}
    }
    
    // Accept a connection to the server.
    private void handleAccept(SelectionKey key) throws IOException 
    {
        SocketChannel sc = ((ServerSocketChannel)key.channel()).accept();
        String address = (new StringBuilder(sc.socket().getInetAddress().toString())).append(":").append(sc.socket().getPort()).toString();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ, address);
        ByteBuffer nickMsg = ByteBuffer.wrap("Please enter your nick as follows: NICK <nickname>\n".getBytes());
        sc.write(nickMsg);
        nickMsg.rewind();
        System.out.println("accepted connection from: " + address);
    }
 
    // Assign a nickname to a given IP address.
    public String assignNick(String nick, SelectionKey key)
    {
    	// Get rid of whitespace (\n, etc...)
    	nick = nick.trim();
    	if (!nicks.contains(nick))
    	{
    		nicks.add(nick);
    		nickPairs.put(key.attachment().toString(), nick);
    		return "You are now known as " + nick + '\n';
    	}
    	else
    		return "Sorry, that nick is taken.\n";
    }
    
    // Read from a socket and multiplex to determine how to handle the message.
    private void handleRead(SelectionKey key) throws IOException 
    {
        String msg;
    	
    	// By default, only broadcast to the message sender...
    	broadcastCode = 4;
        SocketChannel ch = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();
 
        buf.clear();
    	@SuppressWarnings("unused")
		int read = 0;
    	try 
        {
	        while((read = ch.read(buf)) > 0) 
	        {
	            buf.flip();
	            byte[] bytes = new byte[buf.limit()];
	            buf.get(bytes);
	            sb.append(new String(bytes));
	            buf.clear();

	            // Split on ; symbol, so that we can take the broadcastCode, the list of recipients, and the message.
	            // KEY: 
	            // rcvd[0] = broadcastCode
	            // rcvd[1] = special message
	            // rcvd[2] = list of recipients
	            // rcvd[3] = message
	            String[] rcvd = new String[4];
	            String specialCode = "";
	            String message = "";
	            List<String> peerList = new ArrayList<String>();
	            
	            rcvd = sb.toString().split(";");
	            broadcastCode = Integer.parseInt(rcvd[0]); 
	            specialCode = rcvd[1];
	    	    message = rcvd[3];
	            
	        	msg = "";
	        	switch (specialCode)
	        	{
	        		// Quit IRC.
		        	case "QUIT":
		            {
		                msg = getNick(key) + " has signed off.\n";
		                nickPairs.remove(getNick(key));
		                nicks.remove(getNick(key));
		                ch.close();
		        		break;
		            }
		            
		            // Kick the user...
		        	case "BADWORD":
		        	{
		        		msg = getNick(key) + " has been kicked.\n";
		        		nickPairs.remove(getNick(key));
		        		nicks.remove(getNick(key));
		        		ch.close();
		        	}
		        	
		            // Set a nickname (can only be done once.)
		        	case "NICK":
		        	{
		        		System.out.println(message);
		            	String nick = message;
		            	msg = assignNick(nick, key);
		        		break;
		        	}
		        	
		        	// Switch the current channel.
		        	case "SWITCH":
		        	{
		            	String channelName = rcvd[2].trim();
		            	StringBuilder msgBuilder = new StringBuilder("");
		            	checkChannelsForMatch(channelName);
		            	if (foundChannel)
		            	{
		            		for (String s: channels.get(channelIndex).getMessages())
		            			msgBuilder.append(s + '\n');
		            		msg = msgBuilder.toString();
		            	}
		            	else
		            		msg = "Sorry, that channel does not exist.\nTo create it, please enter JOIN <channelname>.\n";
		        		break;
		        	}
		        	
		        	// List all members of a channel.
		        	case "MEMBERS":
		        	{
		            	String channelName = rcvd[2];
		            	StringBuilder memberList = new StringBuilder("");
		            	memberList.append("Listing members:\n");
		            	checkChannelsForMatch(channelName);
		            	try
		            	{
		    	        	for (String s : channels.get(channelIndex).getChannelMembers())
		    	        		memberList.append(s + '\n');
		    	        	msg = memberList.toString();
		            	}
		            	catch (IndexOutOfBoundsException e) { msg = "ERROR: There are no channels.\n"; }
		        		break;
		        	}
		        	
		        	// Join a channel.
		        	case "JOIN":
		        	{
		        		String channelName = rcvd[2];
		            	foundChannel = false;
		            	
		            	// Attempt to find the channel in the list of current channels...
		            	checkChannelsForMatch(channelName);
		            	
		        		// We have found the channel, join it.
		            	if (foundChannel && (channels.get(channelIndex).isPrivate() == false))
		            	{
		            		msg = "currentChannel=" + channelName + '\n';
		            		channels.get(channelIndex).addChannelMember(getNick(key));
		            	}   
		            	else if (foundChannel && (channels.get(channelIndex).isPrivate() == true))
		            		msg = "Sorry, but that is a private channel...\n";
		            	
		            	// The channel does not exist, create it, and add the creator as a member.
		            	else
		            	{
		            		msg = "currentChannel=" + channelName + '\n';
		            		channels.add(new Channel(channelName, false, getNick(key).trim()));
		            	}
		            	channels.get(channelIndex).setNumMembers(channels.get(channelIndex).getNumMembers() + 1);
		        		break;
		        	}
		        	
		        	// Leave a channel.
		        	case "PART":
		        	{
		            	broadcastCode = 0;
		            	String channelName = rcvd[2];
		            	foundChannel = false;
		            	checkChannelsForMatch(channelName);
		            	if (foundChannel)
		            	{
		            		msg = "Parted from channel " + channelName + '\n';
		            		channels.get(channelIndex).removeChannelMember(getNick(key));
		            		if (channels.get(channelIndex).getNumMembers() > 1)
		            		{
		            			channels.get(channelIndex).setNumMembers(channels.get(channelIndex).getNumMembers() - 1);
		            		}
		            		else
		            			channels.remove(channelIndex);
		            	}
		            	else
		            		msg = "Sorry but you are not a member of the channel " + channelName + '\n';
		        		break;
		        	}
		        	
		        	// List all channels.
		        	case "LIST":
		        	{
		            	StringBuilder channelList = new StringBuilder("");
		            	channelList.append("Current channels:\n");
	
		            	// Add all channels in channel list to the StringBuilder and display.
		            	for (Channel c: channels)
		            		channelList.append(c.getName() + '\n');
		            	
		            	msg = channelList.toString();
		        		break;
		        	}
		        	
		        	// Send message to multiple channels.
		        	case "MULT":
		        	{
		        		List<String> channelsToMsg = new ArrayList<String>();
		        		String[] split = sb.toString().split(";");
		        		// split[0] = broadcastCode
		        		// split[1] = MULT
		        		// split[2] = channelList (comma separated)
		        		// split[3] = message
		        		
		        		String[] channelList = split[2].split(",");
		        		for (int i = 0; i < channelList.length; i++)
		        			channelsToMsg.add(channelList[i]);
		        		
		        		for (String s: channelsToMsg)		        		
			        		for (Channel c: channels)
			        			if (s.equals(c.getName()))
			        			{
			        				c.addMessage(getNick(key).trim() + ":" + split[3].trim());
			        				msg = "newMsg=" + c.getName() + '\n';
			        				broadcastAll(msg);
			        			}
		        		broadcastCode = -1;
		        		break;
		        	}

		        	// Send message to current channel.
		        	case "SINGLE":
		        	{
		        		String[] split = sb.toString().split(";");
		        		// split[0] = broadcastCode
		        		// split[1] = SINGLE
		        		// split[2] = channel 
		        		// split[3] = message
		        		for (Channel c: channels)
			        		if (split[2].equals(c.getName()))
			        		{
			        			c.addMessage(getNick(key).trim() + ":" + split[3].trim());
			        			msg = "newMsg=" + c.getName() + '\n';
			        		}
		        		break;
		        	}
		        	
		        	// Private message
		        	case "MSG":
		        	{
		            	boolean foundNick = false;
		            	String nickToMsg = "";
		            	for (String nick: nickPairs.values())
		            	{
		            		if (nick.equals(rcvd[2]))
		            		{
		            			nickToMsg = nick;
		            			foundNick = true;
		            		}
		            	}
		            	if (!foundNick)
		            		msg = "Sorry, but " + nickToMsg + " is not a registered nick...\n";
		            	else
		            	{
		            		msg += "currentChannel=#" + getNick(key) +  nickToMsg + '\n';
		            		channels.add(new Channel("#" + getNick(key)  + nickToMsg, true, new String[] { getNick(key), nickToMsg.toString() }));
		            		keys.add(nickToMsg);
		            		keys.add(getNick(key));
		            		peerList.add(nickToMsg);
		            		peerList.add(getNick(key));
		            	}
		        		break;
		        	}
		        	
		        	default:
		        	{
		        		broadcastCode = Integer.parseInt(rcvd[0]);
		        		msg = getNick(key) + ":" + rcvd[3];
		        		break;
		        	}
	        	}
	        	
	            /* Figure out which mode to broadcast in:
	             * 0 = broadcast only to sender.
	             * 1 = broadcast to all.
	             * 2 = broadcast to peer list.
	             */
	            switch (broadcastCode)
	            {
	            	case 0:
	            		broadcast(msg, key);
	            		break;
	            	case 1:
	            		broadcastAll(msg);
	            		break;
	            	case 2:
	            		broadcastList(msg, peerList);
	            		break;
	            	default:
	                    break;
	            }

	        }
        }
        catch (IOException e) {  }
    }
 
    // Broadcast to a single client.
    private void broadcast(String msg, SelectionKey key)
    {
    	try
    	{
	    	ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());
	        SocketChannel sch = (SocketChannel) key.channel();
	        sch.write(msgBuf);
	        msgBuf.rewind();
    	}
    	catch (ClosedChannelException e)
    	{
    		System.out.println("Client disconnected.");
    	}
    	catch (IOException e) 
    	{ 
    		System.out.println("broadcast: Client crashed..."); 
    		e.printStackTrace();
    	}
    }
        
    // Broadcast to all clients.
    // Running time: O(n)
    private void broadcastAll(String msg)
    {
    	// Iterate over all sockets...
	    for (SelectionKey key : selector.keys()) 
	    	if(key.isValid() && key.channel() instanceof SocketChannel) 
		    	broadcast(msg, key);
	}
 
    // Broadcast to a list of clients.
    // Running time: O(n^2)
    // Inefficient, but does the job.
    public void broadcastList(String msg, List<String> peerList)
    {
    	// Iterate over all sockets...
	    for (SelectionKey key : selector.keys()) 
	    	if(key.isValid() && key.channel() instanceof SocketChannel) 
	    		if (peerList.size() > 0)
		    		for (String s: peerList)
		    			if (s.equals(getNick(key)))
		    				broadcast(msg, key);    	
    }

    public static void main(String[] args) throws IOException 
    {
        new Server(5000);
    }
}