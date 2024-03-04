package ch.uzh.ifi.hase.soprafs24.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.uzh.ifi.hase.soprafs24.constant.Permissions;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.NotFoundException;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockBean private UserService userService;

  /**
   * verifies that the auth request with valid username and password is successful with
   * @throws Exception
   */
  @Test
  public void authUser_success() throws Exception {
    String token = "1";

    // given
    User user = new User();
    user.setUsername("testUsername");
    user.setToken(token);

    given(userService.isUserAuthorized(Mockito.anyString(), Mockito.anyString())).willReturn(user);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setPassword("1234");
    userPostDTO.setUsername("testUsername");

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users/auth")
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(asJsonString(userPostDTO))
                                                    .header("Authorization", token);

    mockMvc.perform(postRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", is(user.getToken())));
  }

  /**
   * verifies that the auth request with invalid username and password returns the expected error
   * @throws Exception
   */
  @Test
  public void authUser_usernamePassword_invalidInput_throwsException() throws Exception {
    String invalidUsername = "invalidUsername";
    String invalidPassword = "invalidPassword";

    // given
    String token = "1";
    User user = new User();
    user.setUsername("turing");
    user.setPassword("enigma123");
    user.setToken(token);

    given(userService.isUserAuthorized(Mockito.eq(invalidUsername), Mockito.eq(invalidPassword)))
        .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setPassword(invalidPassword);
    userPostDTO.setUsername(invalidUsername);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users/auth")
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(asJsonString(userPostDTO))
                                                    .header("Authorization", token);

    mockMvc.perform(postRequest).andExpect(status().isUnauthorized());
  }

  /**
   * verifies that the auth request with no username,password or token returns the expected error
   * @throws Exception
   */
  @Test
  public void authUser_noInput_throwsException() throws Exception {
    // given
    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setPassword("");
    userPostDTO.setUsername("");

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users/auth")
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(asJsonString(userPostDTO));

    mockMvc.perform(postRequest).andExpect(status().isBadRequest());
  }

  /**
   * verifies that the auth requests with valid token is successful
   * @throws Exception
   */
  @Test
  public void authUser_validToken_success() throws Exception {
    String token = "1";

    // given
    User user = new User();
    user.setUsername("testUsername");
    user.setToken(token);

    given(userService.isAuthorized(Mockito.anyString(), Mockito.eq(Permissions.READ)))
        .willReturn(true);
    given(userService.getUserByToken(Mockito.anyString())).willReturn(user);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setToken(token);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users/auth")
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(asJsonString(userPostDTO))
                                                    .header("Authorization", token);

    mockMvc.perform(postRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", is(user.getToken())));
  }

  /**
   * verifies that the auth request with an invalid token returns the expected error
   * @throws Exception
   */
  @Test
  public void authUser_invalidToken_throwsException() throws Exception {
    String invalidToken = "2";
    String token = "1";

    // given
    User user = new User();
    user.setUsername("testUsername");
    user.setToken(token);

    given(userService.isAuthorized(Mockito.eq(invalidToken), Mockito.eq(Permissions.READ)))
        .willReturn(false);
    // given(userService.getUserByToken(Mockito.anyString())).willReturn(user);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setToken(invalidToken); // invalid token

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users/auth")
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(asJsonString(userPostDTO))
                                                    .header("Authorization", token);

    mockMvc.perform(postRequest).andExpect(status().isForbidden());
  }

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    String token = "1";

    // given
    User user = new User();
    user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);
    user.setToken(token);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest =
        get("/users").contentType(MediaType.APPLICATION_JSON).header("Authorization", token);
    given(userService.isAuthorized(Mockito.eq(token),
              Mockito.eq(Permissions.READ)))
        .willReturn(true); // valid token

    // then
    mockMvc.perform(getRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is(user.getName())))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }

  /**
   * verifies that when getting all users with an invalid token, the expected error is thrown
   */
  @Test
  public void givenUsers_whenGetUsers_invalidToken_expectedException() throws Exception {
    String invalidToken = "2";

    given(userService.isAuthorized(Mockito.anyString(), Mockito.any(Permissions.class)))
        .willReturn(false);

    MockHttpServletRequestBuilder getRequest =
        get("/users").contentType(MediaType.APPLICATION_JSON).header("Authorization", invalidToken);

    mockMvc.perform(getRequest).andExpect(status().isForbidden());
  }

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setName("Test User");
    userPostDTO.setUsername("testUsername");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest =
        post("/users").contentType(MediaType.APPLICATION_JSON).content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  /**
   * verifies that the get request with valid user id is successful and returns the correct user
   * properties
   * @throws Exception
   */
  @Test
  public void getUserWithId_validInput_valid() throws Exception {
    Long userId = 1L;
    String token = "1";

    // given
    User user = new User();
    user.setId(userId);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setToken(token);
    user.setStatus(UserStatus.ONLINE);

    given(userService.getUserById(Mockito.any(Long.class))).willReturn(user);
    given(userService.isAuthorized(Mockito.eq(token),
              Mockito.eq(Permissions.READ)))
        .willReturn(true); // valid token

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder getRequest =
        get("/users/" + userId.toString()).header("Authorization", token);

    mockMvc.perform(getRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  /**
   * verifies that the request for a user an invalid token returns the expected error
   * @throws Exception
   */
  @Test
  public void getUserWithId_invalidToken_expectedError() throws Exception {
    Long userId = 2L;
    String token = "1";

    // given
    User user = new User();
    user.setId(userId);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setToken(token);
    user.setStatus(UserStatus.ONLINE);

    given(userService.getUserById(Mockito.any(Long.class)))
        .willThrow(new NotFoundException("not found"));
    given(userService.isTokenInDB(Mockito.eq(token))).willReturn(false); // invalid token

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder getRequest =
        get("/users/" + userId.toString()).header("Authorization", token);

    mockMvc.perform(getRequest).andExpect(status().isForbidden());
  }

  /**
   * verifies that the request for a user with an invalid user id returns the expected error
   */
  @Test
  public void getUserWithId_invalidUserId_expectedError() throws Exception {
    Long invalidUserId = 2L;
    Long userId = 1L;
    String token = "1";

    // given
    User user = new User();
    user.setId(userId);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setToken(token);
    user.setStatus(UserStatus.ONLINE);

    given(userService.getUserById(Mockito.any(Long.class)))
        .willThrow(new NotFoundException("not found"));
    given(userService.isAuthorized(Mockito.eq(token),
              Mockito.eq(Permissions.READ)))
        .willReturn(true); // valid token

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder getRequest =
        get("/users/" + invalidUserId.toString()).header("Authorization", token);

    mockMvc.perform(getRequest).andExpect(status().isNotFound());
  }

  /**
   * verifies that the put requests for a valid user works and correctly updates the user
   */
  @Test
  public void editUser_validInput_valid() throws Exception {
    Long userId = 1L;
    String token = "1";

    // given
    User user = new User();
    user.setId(userId);
    user.setName("turing");
    user.setUsername("enigma123");
    user.setToken(token);
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setName(user.getName());
    userPostDTO.setUsername(user.getUsername());

    // when user is updated (input is this user) then service returns the updated user
    given(userService.updateUser(Mockito.any(User.class), Mockito.eq(userId), Mockito.eq(token)))
        .willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder putRequest = put("/users/" + userId.toString())
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(asJsonString(user))
                                                   .header("Authorization", token);

    mockMvc.perform(putRequest)
        .andExpect(status().isNoContent())
        .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  /**
   * verifies that the put request for an invalid user id returns the expected error
   */
  @Test
  public void editUser_invalidInput_invalid() throws Exception {
    Long invalidUserId = 2L;
    Long userId = 1L;
    String token = "1";

    // given
    User user = new User();
    user.setId(userId);
    user.setName("turing");
    user.setUsername("enigma123");
    user.setToken(token);
    user.setStatus(UserStatus.ONLINE);

    given(userService.updateUser(
              Mockito.any(User.class), Mockito.eq(invalidUserId), Mockito.anyString()))
        .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder putRequest = put("/users/" + invalidUserId.toString())
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(asJsonString(user))
                                                   .header("Authorization", token);

    mockMvc.perform(putRequest).andExpect(status().isNotFound());
  }

  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username":
   * "testUsername"}
   *
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}
