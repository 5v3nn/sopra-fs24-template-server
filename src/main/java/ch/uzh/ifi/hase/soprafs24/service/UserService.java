package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Permissions;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.NotFoundException;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
     * This is a helper method that will check the uniqueness criteria of the
     * username and the name
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

        String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
        if (userByUsername != null && userByName != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(baseErrorMessage, "username and the name", "are"));
        }
        else if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
        }
        // i want users to have the same name
//        else if (userByName != null) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
//        }
    }

    public boolean isTokenInDB(String userToken) {

        try {
            User userOptional = getUserByToken(userToken);
            return true;
        }
        catch (NotFoundException e) {
            return false;
        }
    }

    public boolean isTokenCorrespondingToUserId(String userToken, Long userId) {
        try {
            User user = getUserByToken(userToken);
            return Objects.equals(user.getId(), userId);
        }
        catch (NotFoundException e) {
            return false;
        }
    }

    // i don't like this how this class is so exposed with so many public methods
    public boolean isAuthorized(String token, Permissions permissions) {
        if (!Objects.equals(token, "") && permissions == Permissions.READ) {
            return isTokenInDB(token);
        }
        return false;
    }

    public boolean isAuthorized(String token, Permissions permissions, Long userId) {
        if (!Objects.equals(token, "") && permissions == Permissions.READ_WRITE) {
            return isTokenCorrespondingToUserId(token, userId);
        }
        return false;
    }

    public User isUserAuthorized(String username, String password) {
        User userUsername = this.userRepository.findByUsername(username);
        // todo find by password

        // todo of if userPassword is null
        if (userUsername == null /* || userPassword == null || userUsername.getUsername() != userPassword.getUsername() */) {
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
