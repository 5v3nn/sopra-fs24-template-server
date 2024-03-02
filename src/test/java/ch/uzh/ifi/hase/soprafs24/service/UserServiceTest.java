package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;

import ch.uzh.ifi.hase.soprafs24.constant.Permissions;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.NotFoundException;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

public class UserServiceTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // given
    testUser = new User();
    testUser.setId(1L);
    testUser.setName("testName");
    testUser.setUsername("testUsername");

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // then
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getName(), createdUser.getName());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
  }

  //    @Test
  //    public void createUser_duplicateName_throwsException() {
  //        // given -> a first user has already been created
  //        userService.createUser(testUser);
  //
  //        // when -> setup additional mocks for UserRepository
  //        Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
  //        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
  //
  //        // then -> attempt to create second user with same user -> check that an error
  //        // is thrown
  //        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  //    }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void updateUser_validInput_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // mock findById to find created user
    Mockito.when(userRepository.findById(Mockito.eq(createdUser.getId())))
        .thenReturn(Optional.of(createdUser));
    Mockito.when(userRepository.findByToken(Mockito.eq(createdUser.getToken())))
        .thenReturn(createdUser);
    List<User> usersSameUsername = new ArrayList<>();
    usersSameUsername.add(createdUser);
    Mockito.when(userRepository.findAllByUsername(Mockito.eq(createdUser.getUsername())))
        .thenReturn(usersSameUsername);

    // update testUser
    createdUser.setBirthday("1923-06-23");
    createdUser.setName("Turing");
    // update call
    User updatedUser =
        userService.updateUser(createdUser, createdUser.getId(), createdUser.getToken());

    // then
    Mockito.verify(userRepository, Mockito.times(2)).save(Mockito.any());

    assertEquals(createdUser.getId(), updatedUser.getId());
    assertEquals(createdUser.getName(), updatedUser.getName());
    assertEquals(createdUser.getUsername(), updatedUser.getUsername());
    assertNotNull(updatedUser.getToken());
    assertEquals(UserStatus.OFFLINE, updatedUser.getStatus());
  }

  @Test
  public void updateUser_validInput_newUsername_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // mock findById to find created user
    Mockito.when(userRepository.findById(Mockito.eq(createdUser.getId())))
        .thenReturn(Optional.of(createdUser));
    Mockito.when(userRepository.findByToken(Mockito.eq(createdUser.getToken())))
        .thenReturn(createdUser);
    List<User> usersSameUsername = new ArrayList<>(); // no other users
    Mockito.when(userRepository.findAllByUsername(Mockito.anyString()))
        .thenReturn(usersSameUsername);

    // update testUser
    createdUser.setUsername("Turing's new username");
    // update call
    User updatedUser =
        userService.updateUser(createdUser, createdUser.getId(), createdUser.getToken());

    // then
    Mockito.verify(userRepository, Mockito.times(2)).save(Mockito.any());

    assertEquals(createdUser.getId(), updatedUser.getId());
    assertEquals(createdUser.getName(), updatedUser.getName());
    assertEquals(createdUser.getUsername(), updatedUser.getUsername());
    assertNotNull(updatedUser.getToken());
    assertEquals(UserStatus.OFFLINE, updatedUser.getStatus());
  }

  @Test
  public void updateUser_validInput_newUsername_throwsException() {
    // when -> any object is being save in the userRepository -> return the dummy
    String sameUsername = "fancyName";

    // testUser
    User createdUser = userService.createUser(testUser);

    // mock findById to find created user
    Mockito.when(userRepository.findById(Mockito.eq(createdUser.getId())))
        .thenReturn(Optional.of(createdUser));
    Mockito.when(userRepository.findByToken(Mockito.eq(createdUser.getToken())))
        .thenReturn(createdUser);
    List<User> usersSameUsername = new ArrayList<>(); // no other users
    User duplacteUserUsername = new User();
    duplacteUserUsername.setUsername(sameUsername);
    usersSameUsername.add(duplacteUserUsername);
    Mockito.when(userRepository.findAllByUsername(Mockito.anyString()))
        .thenReturn(usersSameUsername);

    // update testUser
    createdUser.setUsername(sameUsername); // causes conflict

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class,
        () -> userService.updateUser(createdUser, createdUser.getId(), createdUser.getToken()));
  }

  @Test
  public void updateUser_validInput_notAuthorized_throwsException() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // mock findById to find created user
    Mockito.when(userRepository.findById(Mockito.eq(createdUser.getId())))
        .thenReturn(Optional.of(createdUser));
    Mockito.when(userRepository.findByToken(Mockito.eq(createdUser.getToken())))
        .thenReturn(createdUser);
    List<User> usersSameUsername = new ArrayList<>();
    usersSameUsername.add(createdUser);
    Mockito.when(userRepository.findAllByUsername(Mockito.eq(createdUser.getUsername())))
        .thenReturn(usersSameUsername);

    // update testUser
    createdUser.setBirthday("1923-06-23");
    createdUser.setName("Turing");
    // update call
    assertThrows(ResponseStatusException.class,
        () -> userService.updateUser(createdUser, createdUser.getId(), "invalid-token"));
  }

  @Test
  public void updateUser_invalidInput_noUserToEdit_throwsException() {
    // did not create a user but still try to update -> check that an error is thrown

    // mock findById to find created user
    // return no user if want to call findById
    Mockito.when(userRepository.findById(Mockito.anyLong())).thenReturn(null);

    // update testUser
    User newUser = new User();
    newUser.setBirthday("1923-06-23");
    newUser.setName("Turing");
    // update call
    assertThrows(ResponseStatusException.class,
        () -> userService.updateUser(newUser, newUser.getId(), newUser.getToken()));
  }

  @Test
  public void updateUser_invalidInput_emptyUsername_throwsException() {
    // did not create a user but still try to update -> check that an error is thrown

    // mock findById to find created user
    // return no user if want to call findById
    Mockito.when(userRepository.findById(Mockito.anyLong())).thenReturn(null);

    // update testUser
    User newUser = new User();
    newUser.setBirthday("1923-06-23");
    newUser.setName("Turing");
    newUser.setUsername("");
    // update call
    assertThrows(ResponseStatusException.class,
        () -> userService.updateUser(newUser, newUser.getId(), newUser.getToken()));
  }

  @Test
  public void getUserById_validInputs_success() {
    Long userId = 1L;
    Mockito.when(userRepository.findById(Mockito.eq(userId)))
        .thenReturn(Optional.ofNullable(testUser));

    // insert test user
    // assume this works todo remove
    User createdUser = userService.createUser(testUser);
    User getUser = userService.getUserById(userId);

    assertEquals(createdUser, getUser);
    assertEquals(createdUser.getId(), getUser.getId());
  }

  @Test
  public void getUserById_validInputs_notFoundError() {
    // this user id does not exist
    Long userId = 2L;
    Mockito.when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act and Assert
    assertThrows(NotFoundException.class, () -> { userService.getUserById(userId); });
  }

  @Test
  public void isUserTokenInDB_success() {
    String userToken = "token";
    testUser.setToken(userToken);
    Mockito.when(userRepository.findByToken(Mockito.eq(userToken))).thenReturn(testUser);

    // call
    assertTrue(userService.isTokenInDB(userToken));
  }

  @Test
  public void isUserTokenInDB_failed() {
    String userToken = "token";
    testUser.setToken(userToken);
    Mockito.when(userRepository.findByToken(Mockito.eq(userToken))).thenReturn(testUser);

    assertFalse(userService.isTokenInDB("invalid-token"));
  }

  @Test
  public void isUserTokenCorrespondingToId_success() {
    String userToken = "token";
    testUser.setToken(userToken); // id is 1L

    Mockito.when(userRepository.findByToken(Mockito.eq(userToken))).thenReturn(testUser);

    // call
    assertTrue(userService.isTokenCorrespondingToUserId(userToken, 1L));
  }

  @Test
  public void isUserTokenCorrespondingToId_failed() {
    String userToken = "token";
    testUser.setToken(userToken);
    Mockito.when(userRepository.findByToken(Mockito.eq(userToken))).thenReturn(testUser);

    assertFalse(userService.isTokenCorrespondingToUserId("invalid-token", 1L));
  }

  @Test
  public void isAuthorized_readPermissions_valid_success() {
    String inputToken = "1";
    User mockedUser = new User();

    Mockito.when(userRepository.findByToken(Mockito.eq(inputToken))).thenReturn(mockedUser);
    boolean isAuth = userService.isAuthorized(inputToken, Permissions.READ);

    assertTrue(isAuth);
  }

  @Test
  public void isAuthorized_readPermissions_invalid() {
    String inputToken = "1";

    Mockito.when(userRepository.findByToken(Mockito.eq(inputToken))).thenReturn(null);
    boolean isAuth = userService.isAuthorized(inputToken, Permissions.READ);

    assertFalse(isAuth);
  }

  @Test
  public void isAuthorized_readPermissions_invalidPermissions() {
    boolean isAuth = userService.isAuthorized("1", Permissions.READ_WRITE); // wrong permissions
    assertFalse(isAuth);
  }

  @Test
  public void isUserAuthorized_validUsernamePassword() {
    User testUser = new User();
    String username = "Admiral van Schneider";
    String password = "MissSophie1234";
    testUser.setUsername(username);
    testUser.setPassword(password);

    Mockito.when(userRepository.findByUsername(Mockito.eq(username))).thenReturn(testUser);
    User foundUser = userService.isUserAuthorized(username, password);

    assertEquals(foundUser.getUsername(), username);
    assertEquals(foundUser.getPassword(), password);
  }

  @Test
  public void isUserAuthorized_invalidPassword() {
    User testUser = new User();
    String username = "Admiral van Schneider";
    String password = "MissSophie1234";
    testUser.setUsername(username);
    testUser.setPassword(password);

    Mockito.when(userRepository.findByUsername(Mockito.eq(username))).thenReturn(testUser);

    assertThrows(
        ResponseStatusException.class, () -> userService.isUserAuthorized(username, "i am hacker"));
  }
}
