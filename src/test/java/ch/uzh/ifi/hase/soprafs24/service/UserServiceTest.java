package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Permissions;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.NotFoundException;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
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

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

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
    public void getUserById_validInputs_success() {
        Long userId = 1L;
        Mockito.when(userRepository.findById(Mockito.eq(userId))).thenReturn(Optional.ofNullable(testUser));

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
        assertThrows(NotFoundException.class, () -> {
            userService.getUserById(userId);
        });
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
        boolean isAuth = userService.isAuthorized("1", Permissions.READ_WRITE);  // wrong permissions
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

        assertThrows(ResponseStatusException.class,
                () -> userService.isUserAuthorized(username, "i am hacker"));
    }
}
