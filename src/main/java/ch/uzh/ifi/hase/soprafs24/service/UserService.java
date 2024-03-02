package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Permissions;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.NotFoundException;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {
  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User getUserById(long id) {
    // returns an Optional object that may or may not contain the user with the given ID
    Optional<User> userOptional = this.userRepository.findById(id);

    // checks if the optionalUser is present
    if (userOptional.isPresent()) {
      return userOptional.get();
    }

    // user not found
    throw new NotFoundException("User not found with ID: " + id);
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.OFFLINE);
    checkIfUserExists(newUser);
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  /**
   * Update a user. Can only change Username, Name, and Birthday.
   *
   * @param inputUser      user class with attributes to change
   * @param id             id of user to change
   * @param inputUserToken token passed in Authorized header
   * @return changed user (should be same as inputUser)
   */
  public User updateUser(User inputUser, Long id, String inputUserToken) {
    Optional<User> foundUserOptional = userRepository.findById(id);
    User foundUser;

    // check if username is empty string
    if (Objects.equals(inputUser.getUsername(), "") || inputUser.getUsername() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
    }

    // checks if the optionalUser is present
    if (foundUserOptional.isPresent()) {
      foundUser = foundUserOptional.get();
    } else {
      String errorString = "User with id " + id + " was not found";
      log.debug(errorString);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorString);
    }

    if (!isAuthorized(inputUserToken, Permissions.READ_WRITE, id)) {
      System.out.println("Invalid token");
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this user.");
    }

    // check uniqueness criteria
    //        checkIfUserExists(foundUser);
    List<User> usersByUsername = userRepository.findAllByUsername(inputUser.getUsername());
    // if user with username already exists, and is a different user
    if (!usersByUsername.isEmpty()) {
      for (User user : usersByUsername) {
        if (!Objects.equals(user.getId(), id)) {
          System.out.println("Username already used");
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already used.");
        }
      }
    }

    // update found user with new values
    foundUser.setUsername(inputUser.getUsername());
    foundUser.setName(inputUser.getName());
    foundUser.setBirthday(inputUser.getBirthday());

    // save to db
    foundUser = userRepository.save(foundUser);
    userRepository.flush();

    log.debug("Updated Information for User: {}", inputUser);
    return foundUser;
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username (and the name)
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
    User userByName = userRepository.findByName(userToBeCreated.getName());

    String baseErrorMessage = "The %s provided %s not unique. "
        + "Therefore, the user could not be created!";
    if (userByUsername != null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
    }
    // i want users to have the same name
    //        else if (userByName != null) {
    //            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
    //            String.format(baseErrorMessage, "name", "is"));
    //        }
  }

  public boolean isTokenInDB(String userToken) {
    try {
      User userOptional = getUserByToken(userToken);
      return true;
    } catch (NotFoundException e) {
      return false;
    }
  }

  public boolean isTokenCorrespondingToUserId(String userToken, Long userId) {
    try {
      User user = getUserByToken(userToken);
      return Objects.equals(user.getId(), userId);
    } catch (NotFoundException e) {
      return false;
    }
  }

  // i don't like this how this class is so exposed with so many public methods

  /**
   * Check if token is in DB for READ permissions
   *
   * @param token       user token which has to be valid (in DB)
   * @param permissions currently only READ permissions allowed
   * @return true if token is in DB
   */
  public boolean isAuthorized(String token, Permissions permissions) {
    System.out.println("service: Check if authorized with token='" + token + "' and permissions='"
        + permissions.toString() + "'");
    if (!Objects.equals(token, "") && permissions == Permissions.READ) {
      return isTokenInDB(token);
    }
    return false;
  }

  /**
   * This is used to check READ_WRITE permission for a specific userId
   *
   * @param token       user token for the userId
   * @param permissions Permissions (currently READ_WRITE)
   * @param userId      user id to edit
   * @return true if token corresponds to userId
   */
  public boolean isAuthorized(String token, Permissions permissions, Long userId) {
    if (!Objects.equals(token, "") && permissions == Permissions.READ_WRITE) {
      return isTokenCorrespondingToUserId(token, userId);
    }
    return false;
  }

  public User isUserAuthorized(String username, String password) {
    User userUsername = this.userRepository.findByUsername(username);

    //        if (userUsername == null) {
    //            System.out.println("Did not find user with username='" + username + "'");
    //        }
    //        else {
    //            System.out.println("Found user with username='" + userUsername.getUsername()
    //                    + "' and password='" + userUsername.getPassword()
    //                    + "', but given password='" + password + "'");
    //        }

    // if no user found with username, or if found user has not password given
    if (userUsername == null || !Objects.equals(userUsername.getPassword(), password)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Username or password are wrong");
    }
    return userUsername;
  }

  public User getUserByToken(String token) {
    User user = this.userRepository.findByToken(token);

    if (user != null) {
      return user;
    }
    throw new NotFoundException("user token not found");
  }
}
