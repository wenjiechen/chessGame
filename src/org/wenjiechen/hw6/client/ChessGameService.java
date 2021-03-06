package org.wenjiechen.hw6.client;

import org.shared.chess.Move;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("chessGames")
public interface ChessGameService extends RemoteService{
	/**
	 * check if user logged in, and return token to create Channel 
	 * @return
	 */
	public String creatChannelForLoggedInUser(String playerEmail);
	
	/**
	 * generate log out link for current user
	 */
	public String LogOutLinkGenerator(String redirectURL);
	
	/**
	 * generate invitation link for current user
	 * @param redirectURL
	 * @return
	 */
	public String LogInLinkGenerator(String redirectURL);
	/**
	 * client send move to server
	 * @param move
	 */
	public void sendMoveAndState(String email, String move);
	
	/**
	 * if user logged in and matched, delete the matched pairs
	 */
	public void deleteMatch(String matchid);
	
	public void autoMatch(String email);
	
//	public void removeUserFromWaitinglist();
	
	/**
	 * 
	 * @return all on going matches for current player
	 */
	public String fetchMatchList(String email);
	
	public void emailMatch(String plyaerEmail,String inputemail);
	
	public void getLoginPlayerInfo();
	
	public void calculateRank(String matchid,String gameResult);
	
}
