package org.wenjiechen.hw6.server;

import org.wenjiechen.hw6.client.LoginInfo;
import org.wenjiechen.hw6.client.LoginService;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class LoginServiceImpl extends RemoteServiceServlet implements LoginService {
  
  private final UserService userService = UserServiceFactory.getUserService();

  public LoginInfo login(String requestUri) {
	System.out.println("********login info 1");
    User user = userService.getCurrentUser();
    if (user == null) {
      LoginInfo info = new LoginInfo();
      info.setLoginUrl(userService.createLoginURL(requestUri));
  	  System.out.println("********login info 2");
  	  return info;
    }

    LoginInfo info = new LoginInfo(user.getEmail(), user.getNickname());
    info.setLogoutUrl(userService.createLogoutURL(requestUri));
	System.out.println("********login info 3");
    return info;
  }
}