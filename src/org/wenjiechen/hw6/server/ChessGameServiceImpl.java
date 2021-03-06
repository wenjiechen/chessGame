package org.wenjiechen.hw6.server;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.shared.chess.Color;
import org.shared.chess.Move;
import org.shared.chess.PieceKind;
import org.shared.chess.Position;
import org.shared.chess.StateChanger;
import org.wenjiechen.hw2.StateChangerImpl;
import org.wenjiechen.hw3.HistoryCoder;
import org.wenjiechen.hw6.client.ChessGameService;
import org.wenjiechen.hw6.client.MoveParser;
import org.wenjiechen.hw7.client.Match;
import org.wenjiechen.hw7.client.Player;

import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class ChessGameServiceImpl extends RemoteServiceServlet implements
			ChessGameService {
	
	private static final long serialVersionUID = -11048566576274207L;

	static {
		ObjectifyService.register(Player.class);
		ObjectifyService.register(Match.class);
	}
	
	UserService userService = UserServiceFactory.getUserService();
	ChannelService channelService = ChannelServiceFactory.getChannelService();
	private static Set<Key<Player>> playerWL = new LinkedHashSet<Key<Player>>();
	
	/**
	 * generate a unique token for each logged in user and client use this token open a socketListerner
	 * getuserId returns a long integer
	 * getNickname returns user email
	 */
	@Override
	public String creatChannelForLoggedInUser(String playerEmail){
		System.out.println("playerEmail = " + playerEmail);
		if (playerEmail == null || playerEmail == "") {
			return null;
		}
//		if(userService.isUserLoggedIn() == true){
//			User user = userService.getCurrentUser();
//			Key<Player> playerKey = Key.create(Player.class,user.getEmail());
			Key<Player> playerKey = Key.create(Player.class,playerEmail);
			Player player = ofy().load().key(playerKey).get();
			System.out.println("1");

			//print out this player to check out
			if(player == null){
//				player = new Player(user.getEmail(), user.getNickname());//.substring(0, user.getNickname().indexOf("@")
				player = new Player(playerEmail, playerEmail);//.substring(0, user.getNickname().indexOf("@")
				ofy().save().entity(player).now();
				System.out.println("2");
			}
			
//			String channelId = user.getEmail() + "_" + String.valueOf(player.getChannelsNum()+1);
			String channelId = playerEmail + "_" + String.valueOf(player.getChannelsNum()+1);
			System.out.println("3");
			String token = channelService.createChannel(channelId);
			String playerInfo = "name=" + player.getName() + "=email=" + player.getEmail()+"=rank="+player.getRank();
			String result = token + ">"+playerInfo;
			System.out.println("4");
			return result;
//		}
		
	}
	
	@Override
	public void getLoginPlayerInfo() {
		if (userService.isUserLoggedIn() == true) {
			System.out.println("getloginplayerinfo()");
			User user = userService.getCurrentUser();
			Player player = ofy().load()
					.key(Key.create(Player.class, user.getEmail())).get();
			for (String c : player.getAllChannels()) {
				channelService.sendMessage(new ChannelMessage(c,
						"currentPlayerMes>name=" + player.getName() + "email="
								+ player.getEmail()));
			}
		}
	}

	/**
	 * @param redirectURL where when after log out redirect to.
	 * @return
	 */
	@Override
	public String LogOutLinkGenerator(String redirectURL){
		String link = null;
		if(userService.isUserLoggedIn()){
			link =userService.createLogoutURL(redirectURL);
		}
		return link;
	}
	
	@Override
	public String LogInLinkGenerator(String redirectURL){
		String link = null;
		if(userService.isUserLoggedIn()){
			link = userService.createLoginURL(redirectURL);
		}
		return link;
	}
	
	@Override
	public String fetchMatchList(String email) {
		System.out.println("6");
		String matchList = "";  //the best way to initialize String
		if(email == null || email == ""){
			return matchList;
		}
//		if (userService.isUserLoggedIn()) {
//			User user = userService.getCurrentUser();
			Key<Player> curPlayerKey = Key.create(Player.class, email);
			Player player = ofy().load().key(Key.create(Player.class, email)).get();

			// check out if the player is NULL
			for (Match match : ofy().load().keys(player.getAllMatches())
					.values()) {
				Player opponent = ofy().load().key(match.getOtherPlayer(curPlayerKey)).get();

				matchList += "matchId=" + match.getMatchId().toString();
				if (match.getColor(curPlayerKey) == Color.WHITE) {
					matchList += "=color=white";
				} else {
					matchList += "=color=black";
				}
				matchList += "=opponentName=" + opponent.getName();
				matchList += "=opponentRank=" + opponent.getRank();
				matchList += "=state=" + match.getState();
				//dow mon dd hh:mm:ss zzz yyyy
				matchList += "=date="+match.getDate().toString();
				// array segment note
				matchList += "#";
			
			}
			System.out.println("returned matchlist is:"+ matchList);
			return matchList;
//		}
		//System.out.println("returned \"\" matchlist is:"+ matchList);
	
	}
	
	//two player only have one ongoing match, and only one matchID
	public void emailMatch(String plyaerEmail, String email){
		
//		if (userService.isUserLoggedIn()) {
//			User user = userService.getCurrentUser();
		
			Key<Player> curplayerKey = Key.create(Player.class, plyaerEmail);
			Player curplayer = ofy().load().key(curplayerKey).get();
			Key<Player> opponentPlayerKey = Key.create(Player.class, email);
			Player opponentPlayer = ofy().load().key(opponentPlayerKey).get();
			//check if opponent has been in current player's match list
			Set<Key<Match>> matchs = curplayer.getAllMatches();
			
			for(Match m : ofy().load().keys(matchs).values()){
				if(m.getBlackPlayer().equals(opponentPlayerKey) || m.getWhitePlayer().equals(opponentPlayerKey)){
					String message = "emailMatch>haveMatchedBefore";
					for (String channel : curplayer.getAllChannels()) {
						channelService.sendMessage(new ChannelMessage(channel,message));
					}
					return;
				}
			}
			
			if (opponentPlayer == null) {
				opponentPlayer = new Player(email, email.substring(0, email.indexOf("@")));
				ofy().save().entity(opponentPlayer).now();			
			}
			
			Match match = new Match(curplayerKey, opponentPlayerKey, "newgame");
			Key<Match> matchKey = ofy().save().entity(match).now();
			
			curplayer.addMatch(matchKey);
			opponentPlayer.addMatch(matchKey);
			ofy().save().entities(curplayer, opponentPlayer).now();
			
			String message = "emailMatch>" + "matchId=" + match.getMatchId() + "=color=white" + "=opponentName=" 
								+ opponentPlayer.getName() + "=opponentRank=" + opponentPlayer.getRank();
			for (String channel : curplayer.getAllChannels()) {
				channelService.sendMessage(new ChannelMessage(channel, message));
			}
			message = "emailMatch>"+ "matchId=" + match.getMatchId() + "=color=black" + "=opponentName=" 
								+ curplayer.getName() + "=opponentRank=" + curplayer.getRank();
			for (String channel : opponentPlayer.getAllChannels()) {
				channelService.sendMessage(new ChannelMessage(channel, message));
			}
//		}		
	}
	
	
	/**
	 * get matchid from client, delete this match and send message to player to refresh display
	 */
	@Override
	public void deleteMatch(String matchid){
//		if (userService.isUserLoggedIn()) {
			
		Key<Match> matchkey = Key.create(Match.class,Long.parseLong(matchid));
			Match match = ofy().load().key(matchkey).get();
			Player wplayer = ofy().load().key(match.getWhitePlayer()).get();
			Player bplayer = ofy().load().key(match.getBlackPlayer()).get();
			wplayer.removeMatch(matchkey);
			bplayer.removeMatch(matchkey);
			ofy().save().entities(wplayer,bplayer).now();
			ofy().delete().entity(match).now();
			//send message
			String message = "deleteMatch>" + matchid;
			for(String c: wplayer.getAllChannels()){
				channelService.sendMessage(new ChannelMessage(c,message));
			}
			for(String c: bplayer.getAllChannels()){
				channelService.sendMessage(new ChannelMessage(c,message));
			}
//		}		
	}
	
//	@Override
//	public void removeUserFromWaitinglist(){
//		if(userService.isUserLoggedIn()){
//			User user = userService.getCurrentUser();
//			Key<Player> playerkey = Key.create(Player.class,user.getEmail());
//			playerWL.remove(playerkey);
//		}		
//	}
	
	/**
	 * ?????why can't invoke Move construction by parseMove?
	 * receive move message from client,
	 * 1 get the match game, make move to change game state, save match
	 * 2 send move message to two players 
	 * moveMes :"matchId="+ 1 +"=move="+ 3 +"=state="+ 5; 
	 */
	@Override
	public void sendMoveAndState(String email, String moveAndStateMes) {
		System.out.println("sendMoveAndState(): " + moveAndStateMes);
//		if (userService.isUserLoggedIn()) {
			String[] mm = moveAndStateMes.split("=");
			Long matchId = Long.parseLong(mm[1]);
			String move = mm[3];
			String state = mm[5];

			//change state of match, and save state back to match
			Match match = ofy().load().key(Key.create(Match.class, matchId)).get();
			match.setState(state);
			ofy().save().entity(match).now();

			Key<Player> curPlayerKey = Key.create(Player.class,email);
			Key<Player> oppPlayerKey = match.getOtherPlayer(curPlayerKey);
			Player oppPlayer = ofy().load().key(oppPlayerKey).get();

			//send move to opponent player
			//sendMove>"matchId="+matchId+"=move="+move.toString()
			String moveMessage = "sendMove<" + "matchId="+mm[1]+"=move=" +move;
			for (String channel : oppPlayer.getAllChannels()) {
				channelService.sendMessage(new ChannelMessage(channel, moveMessage));
			}
//		}
	}
	
	public void calculateRank(String matchid,String gameResult){

		System.out.println("calculateRank( ) matchid "+ matchid);
		Key<Match> matchKey = Key.create(Match.class,Long.parseLong(matchid));
		Match match = ofy().load().key(matchKey).get();
		
		Key<Player> blackPlayerKey = match.getBlackPlayer();
		Player blackPlayer = ofy().load().key(blackPlayerKey).get();
		Key<Player> wpk = match.getWhitePlayer();
        Player whitePlayer=ofy().load().key(wpk).get();
        
        double eB,eW,aB,aW,cB,cW;

        cB=blackPlayer.getRank();
        cW=whitePlayer.getRank();
        
		if (gameResult.equals("black")) {
			aB = 1;
			aW = 0;
		} else if (gameResult.equals("white")) {
			aW = 1;
			aB = 0;

		} else {
			aB = 0.5;
			aW = 0.5;
		}        
        
        eB=1/(1+(double)Math.pow(10, ((double)(cW-cB))/400.00));
        eW=1/(1+(double)Math.pow(10, ((double)(cB-cW))/400.00));
        
        blackPlayer.setRank((int) (cB+(double)15*((double)aB-(double)eB)));
        whitePlayer.setRank((int) (cW+(double)15*((double)aW-(double)eW)));
        
        ofy().save().entity(match).now();
        ofy().save().entities(whitePlayer,blackPlayer).now();
	}
	
	
	/**
	 * find another online player for current player
	 * only unmatched player can use auto match.
	 * if matched player want auto match, he has to click delete match first
	 */
//	private static Set<Key<Player>> playerWL = new LinkedHashSet<Key<Player>>();
	@Override
	public void autoMatch(String email) {
//		if (userService.isUserLoggedIn() == true) {
//			User curUser = userService.getCurrentUser();
			Key<Player> playerKey = Key
					.create(Player.class, email);
			Player player = ofy().load().key(playerKey).get();

			// if palyer waiting list is not empty, search other player
			if (!playerWL.isEmpty()) {
				System.out.println("autoMatch 1");
				Key<Player> oppPlayerKey = null;
				for (Key<Player> wp : playerWL) {
					if (!wp.equals(playerKey)) {
						oppPlayerKey = wp;
						playerWL.remove(oppPlayerKey);
						playerWL.remove(playerKey);
						break;
					}
				}
				// didn't find other player
				if (oppPlayerKey == null) {
					System.out.println("autoMatch 2");
					playerWL.add(playerKey);
					String message = "autoMatch>noOtherPlayer";
					for (String channel : player.getAllChannels()) {
						channelService.sendMessage(new ChannelMessage(channel,
								message));
					}
					return;
				}
				// match success create a new match for them
				// withe,black,state
				Match match = new Match(playerKey, oppPlayerKey, "newgame");
				Key<Match> matchkey = ofy().save().entity(match).now();
				Player oppPlayer = ofy().load().key(oppPlayerKey).get();
				player.addMatch(matchkey);
				oppPlayer.addMatch(matchkey);
				//must save players in datastore
				ofy().save().entities(player,oppPlayer).now();
				
				String message = "autoMatch>" + "matchId=" + match.getMatchId()
						+ "=color=white" 
						+ "=opponentName=" + oppPlayer.getName()
						+ "=opponentRank=" + oppPlayer.getRank();
				for (String channel : player.getAllChannels()) {
					channelService.sendMessage(new ChannelMessage(channel,
							message));
				}
				System.out.println("matchId " + match.getMatchId());
				message = "autoMatch>" 
						+ "matchId=" + match.getMatchId()
						+ "=color=black" 
						+ "=opponentName=" + player.getName()
						+ "=opponentRank=" + player.getRank();
				for (String c : oppPlayer.getAllChannels()) {
					channelService.sendMessage(new ChannelMessage(c, message));
				}
				System.out.println("autoMatch 3");
			} else {
				// playerWL is empty
				playerWL.add(playerKey);
				String message = "autoMatch>noOtherPlayer";
				for (String channel : player.getAllChannels()) {
					channelService.sendMessage(new ChannelMessage(channel,message));
				}
			}//else
//		}//if login
	}
	
//	=====================================================================================
	public static void addHeadersForCORS(HttpServletRequest req,
			HttpServletResponse resp) {
		resp.setHeader("Access-Control-Allow-Methods", "POST"); // "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Headers",
				"X-GWT-Module-Base, X-GWT-Permutation, Content-Type");
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
		addHeadersForCORS(req, resp);
	}

	@Override
	protected void onAfterResponseSerialized(String serializedResponse) {
		super.onAfterResponseSerialized(serializedResponse);
		addHeadersForCORS(getThreadLocalRequest(), getThreadLocalResponse());
	}

	//=====================================================================================
//	public static void addHeadersForCORS(HttpServletRequest req, HttpServletResponse resp) {
//	    resp.setHeader("Access-Control-Allow-Methods", "POST"); // "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");
//	    resp.setHeader("Access-Control-Allow-Origin", "*");
//	    resp.setHeader("Access-Control-Allow-Headers", "X-GWT-Module-Base, X-GWT-Permutation, Content-Type"); 
//	  }
//
//	  @Override
//	  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
//	    addHeadersForCORS(req, resp);
//	  }
//
//	  @Override
//	  protected void onAfterResponseSerialized(String serializedResponse) {
//	    super.onAfterResponseSerialized(serializedResponse);
//	    addHeadersForCORS(getThreadLocalRequest(), getThreadLocalResponse());
//	  }
}